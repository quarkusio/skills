/*
 * Checkpoint.java — migration state manager for migrate-spring-to-quarkus
 *
 * Run as a Java single-file program (JDK 17+, no build step, no dependencies):
 *
 *   java Checkpoint.java init     <target> <source>
 *   java Checkpoint.java status   <target>
 *   java Checkpoint.java start    <target> <module>
 *   java Checkpoint.java complete <target> <module> <completed|skipped>
 *                                 [--gate PASS|SKIP|ALWAYS] [--reason <text>]
 *                                 [--compile PASS|FAIL|NOT_RUN] [--compile-attempts <n>]
 *                                 [--description <text>] [--report <relpath>]
 *   java Checkpoint.java reset    <target> <module>
 *   java Checkpoint.java resume   <target> [--accept-drift] [--max-resumes <n>]
 *
 * Every command prints one JSON document to stdout. Exit code 0 on success, 1 on error
 * (verdict "ERROR"). The context file is replaced atomically (temp file + move).
 */

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class Checkpoint {

    static final String SCHEMA_VERSION = "1.0";
    static final String CONTEXT_REL = "migration-metadata/migration-context.json";

    /** Canonical module execution order (planning is always first after prerequisite). */
    static final List<String> MODULE_ORDER =
            List.of("planning", "build", "persistence", "code", "messaging", "frontend", "testing", "cleanup");

    static final Set<String> FINGERPRINT_EXCLUDED_TOP_LEVEL =
            Set.of(".git", "target", "build", ".gradle", "migration-metadata");

    /** Resumes allowed per migration unless the user raises the limit with 'resume --max-resumes'. */
    static final long DEFAULT_MAX_RESUMES = 5;

    // ------------------------------------------------------------------
    // result / error plumbing
    // ------------------------------------------------------------------

    /** Carries the JSON payload and exit code out of a command. Never serialized. */
    @SuppressWarnings("serial")
    static final class Done extends RuntimeException {
        private static final long serialVersionUID = 1L;
        final int code;
        final transient Map<String, Object> payload;

        Done(int code, Map<String, Object> payload) {
            super(null, null, false, false);
            this.code = code;
            this.payload = payload;
        }
    }

    static Done ok(Map<String, Object> payload) {
        return new Done(0, payload);
    }

    static Done err(String message) {
        return new Done(1, obj("verdict", "ERROR", "message", message));
    }

    static Done err(String message, String key, Object value) {
        return new Done(1, obj("verdict", "ERROR", "message", message, key, value));
    }

    /** Ordered map builder: obj("a", 1, "b", 2). */
    static Map<String, Object> obj(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    // ------------------------------------------------------------------
    // typed access helpers for the parsed JSON tree
    // ------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object o) {
        return (Map<String, Object>) o;
    }

    /** Returns the map stored at key, creating (and storing) an empty one if absent or mistyped. */
    static Map<String, Object> submap(Map<String, Object> m, String key) {
        Object o = m.get(key);
        if (o instanceof Map) {
            return map(o);
        }
        Map<String, Object> fresh = new LinkedHashMap<>();
        m.put(key, fresh);
        return fresh;
    }

    static String now() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
    }

    // ------------------------------------------------------------------
    // context file I/O
    // ------------------------------------------------------------------

    static Path contextPath(Path target) {
        return target.resolve(CONTEXT_REL);
    }

    record Loaded(boolean missing, boolean corrupt, Map<String, Object> ctx) {}

    static Loaded load(Path target) {
        Path p = contextPath(target);
        if (!Files.exists(p)) {
            return new Loaded(true, false, null);
        }
        try {
            Object parsed = Json.parse(Files.readString(p, StandardCharsets.UTF_8));
            if (parsed instanceof Map) {
                return new Loaded(false, false, map(parsed));
            }
        } catch (IOException | RuntimeException e) {
            // fall through: unreadable or unparseable
        }
        return new Loaded(false, true, null);
    }

    static Map<String, Object> require(Path target) {
        Loaded l = load(target);
        if (l.missing()) {
            throw err("No context file found. Run 'init' first.");
        }
        if (l.corrupt()) {
            throw err("Context file is corrupt. Run 'status' and follow the CORRUPT procedure.");
        }
        return l.ctx();
    }

    static void write(Path target, Map<String, Object> ctx) throws IOException {
        Path p = contextPath(target);
        Files.createDirectories(p.getParent());
        Path tmp = Files.createTempFile(p.getParent(), ".ctx-", ".tmp");
        try {
            Files.writeString(tmp, Json.stringify(ctx) + "\n", StandardCharsets.UTF_8);
            try {
                Files.move(tmp, p, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    // ------------------------------------------------------------------
    // domain helpers
    // ------------------------------------------------------------------

    /**
     * Hash the source tree deterministically. Exclusions apply to top-level directories only,
     * so a Java package named "build" is still hashed.
     */
    static String fingerprint(Path source) throws IOException {
        List<String> rels = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(source)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                Path r = source.relativize(p);
                boolean inExcludedTopDir =
                        r.getNameCount() > 1 && FINGERPRINT_EXCLUDED_TOP_LEVEL.contains(r.getName(0).toString());
                if (!inExcludedTopDir) {
                    rels.add(r.toString().replace('\\', '/'));
                }
            });
        }
        Collections.sort(rels);
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            for (String rel : rels) {
                sha.update(rel.getBytes(StandardCharsets.UTF_8));
                sha.update((byte) 0);
                try {
                    sha.update(Files.readAllBytes(source.resolve(rel)));
                } catch (IOException unreadable) {
                    // skip unreadable files, same as an absent file
                }
            }
            return HexFormat.of().formatHex(sha.digest()).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The recorded status of a module (modules.<name>.status), or null if it has no record. */
    static String moduleStatus(Map<String, Object> ctx, String module) {
        if (ctx.get("modules") instanceof Map) {
            Object rec = map(ctx.get("modules")).get(module);
            if (rec instanceof Map && map(rec).get("status") instanceof String) {
                return (String) map(rec).get("status");
            }
        }
        return null;
    }

    /** Modules (in execution order) whose recorded status equals the given one. modules.<name>.status is the only source. */
    static List<String> modulesWithStatus(Map<String, Object> ctx, String status) {
        List<String> out = new ArrayList<>();
        for (String m : MODULE_ORDER) {
            if (status.equals(moduleStatus(ctx, m))) {
                out.add(m);
            }
        }
        return out;
    }

    /** Modules that still have to run: everything not recorded as completed or skipped. */
    static List<String> pending(Map<String, Object> ctx) {
        List<String> out = new ArrayList<>();
        for (String m : MODULE_ORDER) {
            String st = moduleStatus(ctx, m);
            if (!"completed".equals(st) && !"skipped".equals(st)) {
                out.add(m);
            }
        }
        return out;
    }

    static void requireModule(String module) {
        if (!MODULE_ORDER.contains(module)) {
            throw err("Unknown module '" + module + "'. Valid modules: " + MODULE_ORDER);
        }
    }

    static long longOf(Object o, long fallback) {
        return o instanceof Number ? ((Number) o).longValue() : fallback;
    }

    /**
     * Parses "--key value" pairs and bare "--flag" switches (stored as "true").
     * Unknown options and options without a value are hard errors.
     */
    static Map<String, String> parseOpts(String[] args, int from, Set<String> valueOpts, Set<String> flags) {
        Map<String, String> opts = new LinkedHashMap<>();
        int i = from;
        while (i < args.length) {
            String a = args[i];
            String name = a.startsWith("--") ? a.substring(2) : null;
            if (name != null && flags.contains(name)) {
                opts.put(name, "true");
                i++;
                continue;
            }
            if (name == null || !valueOpts.contains(name)) {
                java.util.TreeSet<String> shown = new java.util.TreeSet<>();
                valueOpts.forEach(k -> shown.add("--" + k));
                flags.forEach(k -> shown.add("--" + k));
                throw err("Unknown option '" + a + "'. Allowed: " + shown);
            }
            if (i + 1 >= args.length || args[i + 1].startsWith("--")) {
                throw err("Option '" + a + "' requires a value");
            }
            opts.put(name, args[i + 1]);
            i += 2;
        }
        return opts;
    }

    // ------------------------------------------------------------------
    // commands
    // ------------------------------------------------------------------

    /** Create the initial context file. Called by planning after migration-spec.yaml is written. */
    static Done init(Path target, Path source) throws IOException {
        Path p = contextPath(target);
        if (Files.exists(p)) {
            throw err("Context file already exists; use 'status' to inspect it.", "path", p.toString());
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("schemaVersion", SCHEMA_VERSION);
        ctx.put("generatedAt", now());
        ctx.put("lastCheckpointAt", now());
        ctx.put("resumeCount", 0L);
        ctx.put("maxResumes", DEFAULT_MAX_RESUMES);
        ctx.put("sourceRepo", source.toAbsolutePath().normalize().toString());
        ctx.put("targetRepo", target.toAbsolutePath().normalize().toString());
        ctx.put("sourceFingerprint", fingerprint(source));
        ctx.put("currentModule", null);
        ctx.put("paths", obj(
                "migrationSpec", target.resolve("migration-spec.yaml").toString()));
        Map<String, Object> reports = new LinkedHashMap<>();
        for (String m : MODULE_ORDER) {
            reports.put(m, null);
        }
        ctx.put("moduleReports", reports);
        ctx.put("modules", new LinkedHashMap<String, Object>());
        write(target, ctx);
        return ok(obj("verdict", "CREATED", "context", ctx));
    }

    /** Validate state and return a machine-readable verdict. */
    static Done status(Path target) throws IOException {
        Loaded l = load(target);
        if (l.missing()) {
            return ok(obj("verdict", "NOT_FOUND"));
        }
        if (l.corrupt()) {
            return ok(obj("verdict", "CORRUPT", "reason", "unparseable JSON"));
        }
        Map<String, Object> ctx = l.ctx();

        List<String> missing = new ArrayList<>();
        if (!(ctx.get("modules") instanceof Map)) {
            missing.add("modules");
        }
        if (!(ctx.get("sourceRepo") instanceof String)) {
            missing.add("sourceRepo");
        }
        if (!missing.isEmpty()) {
            return ok(obj("verdict", "CORRUPT", "reason", "missing or invalid fields", "missing_fields", missing));
        }

        List<String> completed = modulesWithStatus(ctx, "completed");
        List<String> skipped = modulesWithStatus(ctx, "skipped");
        Object current = ctx.get("currentModule");

        boolean drift = false;
        Path source = Paths.get((String) ctx.get("sourceRepo"));
        if (ctx.get("sourceFingerprint") instanceof String && Files.isDirectory(source)) {
            drift = !fingerprint(source).equals(ctx.get("sourceFingerprint"));
        }

        String specWarning = null;
        if (completed.contains("planning")) {
            Object specPath = ctx.get("paths") instanceof Map ? map(ctx.get("paths")).get("migrationSpec") : null;
            Path spec = specPath instanceof String ? Paths.get((String) specPath) : target.resolve("migration-spec.yaml");
            if (!Files.exists(spec)) {
                specWarning = "migration-spec.yaml missing despite planning being completed";
            }
        }

        List<String> missingReports = new ArrayList<>();
        Map<String, Object> reports = ctx.get("moduleReports") instanceof Map ? map(ctx.get("moduleReports")) : Map.of();
        for (String m : completed) {
            Object rel = reports.get(m);
            if (rel instanceof String && !Files.exists(target.resolve((String) rel))) {
                missingReports.add(m);
            }
        }

        List<String> pending = pending(ctx);
        if (pending.isEmpty() && current == null) {
            return ok(obj("verdict", "COMPLETE", "completedModules", completed, "skippedModules", skipped));
        }

        return ok(obj(
                "verdict", "RESUMABLE",
                "currentModule", current,
                "completedModules", completed,
                "skippedModules", skipped,
                "pendingModules", pending,
                "modules", ctx.get("modules") instanceof Map ? ctx.get("modules") : new LinkedHashMap<String, Object>(),
                "lastCheckpointAt", ctx.get("lastCheckpointAt"),
                "resumeCount", longOf(ctx.get("resumeCount"), 0L),
                "maxResumes", longOf(ctx.get("maxResumes"), DEFAULT_MAX_RESUMES),
                "resumeLimitReached", longOf(ctx.get("resumeCount"), 0L)
                        >= longOf(ctx.get("maxResumes"), DEFAULT_MAX_RESUMES),
                "drift", drift,
                "specWarning", specWarning,
                "missingReports", missingReports));
    }

    /** Record that a module is about to start. */
    static Done start(Path target, String module) throws IOException {
        requireModule(module);
        Map<String, Object> ctx = require(target);
        String ts = now();
        ctx.put("currentModule", module);
        ctx.put("lastCheckpointAt", ts);
        Map<String, Object> rec = submap(submap(ctx, "modules"), module);
        rec.put("status", "in_progress");
        rec.put("startedAt", ts);
        rec.remove("finishedAt");
        rec.remove("statusDescription");
        write(target, ctx);
        return ok(obj("verdict", "STARTED", "module", module));
    }

    /** Record the outcome of a module in modules.<name>.status (the single source of truth). Idempotent. */
    static Done complete(Path target, String module, String outcome, Map<String, String> opts) throws IOException {
        requireModule(module);
        if (!List.of("completed", "skipped").contains(outcome)) {
            throw err("Invalid outcome '" + outcome + "'. Use: completed | skipped");
        }
        if (opts.containsKey("gate") && !List.of("PASS", "SKIP", "ALWAYS").contains(opts.get("gate"))) {
            throw err("Invalid --gate '" + opts.get("gate") + "'. Use: PASS | SKIP | ALWAYS");
        }
        if (opts.containsKey("compile") && !List.of("PASS", "FAIL", "NOT_RUN").contains(opts.get("compile"))) {
            throw err("Invalid --compile '" + opts.get("compile") + "'. Use: PASS | FAIL | NOT_RUN");
        }
        if (opts.containsKey("compile-attempts") && !opts.get("compile-attempts").matches("\\d{1,9}")) {
            throw err("--compile-attempts must be a non-negative integer");
        }

        Map<String, Object> ctx = require(target);

        if (opts.containsKey("report")) {
            submap(ctx, "moduleReports").put(module, opts.get("report"));
        }

        String ts = now();
        Map<String, Object> rec = submap(submap(ctx, "modules"), module);
        rec.put("status", outcome);
        rec.put("finishedAt", ts);
        if (opts.containsKey("gate")) {
            rec.put("gate", opts.get("gate"));
        }
        if (opts.containsKey("reason")) {
            rec.put("gateReason", opts.get("reason"));
        }
        if (opts.containsKey("compile")) {
            rec.put("compile", opts.get("compile"));
        }
        if (opts.containsKey("compile-attempts")) {
            rec.put("compileAttempts", Long.parseLong(opts.get("compile-attempts")));
        }
        if (opts.containsKey("description")) {
            rec.put("statusDescription", opts.get("description"));
        } else {
            rec.remove("statusDescription"); // never keep a stale description from an earlier status
        }

        if (module.equals(ctx.get("currentModule"))) {
            ctx.put("currentModule", null);
        }
        ctx.put("lastCheckpointAt", ts);
        write(target, ctx);
        return ok(obj("verdict", "RECORDED", "module", module, "outcome", outcome, "record", rec));
    }

    /** Clear a module and every later module so they re-run. */
    static Done reset(Path target, String module) throws IOException {
        requireModule(module);
        Map<String, Object> ctx = require(target);

        List<String> affected = new ArrayList<>(MODULE_ORDER.subList(MODULE_ORDER.indexOf(module), MODULE_ORDER.size()));
        List<String> removed = new ArrayList<>();
        for (String m : affected) {
            String st = moduleStatus(ctx, m);
            if ("completed".equals(st) || "skipped".equals(st)) {
                removed.add(m);
            }
        }
        if (affected.contains(ctx.get("currentModule"))) {
            ctx.put("currentModule", null);
        }
        Map<String, Object> modules = submap(ctx, "modules");
        Map<String, Object> reports = submap(ctx, "moduleReports");
        for (String m : affected) {
            modules.remove(m);
            if (reports.containsKey(m)) {
                reports.put(m, null);
            }
        }
        ctx.put("lastCheckpointAt", now());
        write(target, ctx);
        return ok(obj("verdict", "RESET", "module", module,
                "removedFromState", removed, "pendingModules", pending(ctx)));
    }

    /**
     * Commit to resuming: enforce the resume limit, bump resumeCount, clear the interrupted currentModule,
     * optionally refresh the fingerprint. Refuses without changing state once the limit is reached, so a
     * migration that keeps dying cannot loop forever.
     */
    static Done resume(Path target, Map<String, String> opts) throws IOException {
        if (opts.containsKey("max-resumes") && !opts.get("max-resumes").matches("[1-9]\\d{0,3}")) {
            throw err("--max-resumes must be an integer between 1 and 9999");
        }
        Map<String, Object> ctx = require(target);
        long count = longOf(ctx.get("resumeCount"), 0L);
        if (opts.containsKey("max-resumes")) {
            ctx.put("maxResumes", Long.parseLong(opts.get("max-resumes")));
        }
        long max = longOf(ctx.get("maxResumes"), DEFAULT_MAX_RESUMES);
        if (count >= max) {
            return new Done(1, obj(
                    "verdict", "RESUME_LIMIT_REACHED",
                    "message", "Resume limit reached (" + count + " of " + max + "). STOP the migration now and "
                            + "tell the user. Do not run any further module. The user must inspect " + target
                            + " and, if they want to continue, explicitly approve re-running resume with "
                            + "--max-resumes <n> (n greater than " + count + ").",
                    "resumeCount", count,
                    "maxResumes", max));
        }
        ctx.put("resumeCount", count + 1);
        if (opts.containsKey("accept-drift") && ctx.get("sourceRepo") instanceof String) {
            Path src = Paths.get((String) ctx.get("sourceRepo"));
            if (Files.isDirectory(src)) {
                ctx.put("sourceFingerprint", fingerprint(src));
            }
        }
        Object interrupted = ctx.get("currentModule");
        ctx.put("currentModule", null);
        ctx.put("lastCheckpointAt", now());
        write(target, ctx);
        return ok(obj("verdict", "RESUMED", "resumeCount", count + 1, "maxResumes", max,
                "interruptedModule", interrupted, "pendingModules", pending(ctx)));
    }

    // ------------------------------------------------------------------
    // entrypoint
    // ------------------------------------------------------------------

    static final String USAGE = String.join("\n",
            "Checkpoint.java — migration state manager for migrate-spring-to-quarkus",
            "",
            "Usage (JDK 17+):",
            "  java Checkpoint.java init     <target> <source>",
            "  java Checkpoint.java status   <target>",
            "  java Checkpoint.java start    <target> <module>",
            "  java Checkpoint.java complete <target> <module> <completed|skipped>",
            "        [--gate PASS|SKIP|ALWAYS] [--reason <text>] [--compile PASS|FAIL|NOT_RUN]",
            "        [--compile-attempts <n>] [--description <text>] [--report <relpath>]",
            "  java Checkpoint.java reset    <target> <module>",
            "  java Checkpoint.java resume   <target> [--accept-drift] [--max-resumes <n>]",
            "",
            "Modules: " + String.join(", ", MODULE_ORDER));

    static Done run(String[] args) throws IOException {
        String cmd = args[0];
        switch (cmd) {
            case "init":
                if (args.length != 3) {
                    throw err("Usage: Checkpoint.java init <target> <source>");
                }
                return init(Paths.get(args[1]), Paths.get(args[2]));
            case "status":
                if (args.length != 2) {
                    throw err("Usage: Checkpoint.java status <target>");
                }
                return status(Paths.get(args[1]));
            case "start":
                if (args.length != 3) {
                    throw err("Usage: Checkpoint.java start <target> <module>");
                }
                return start(Paths.get(args[1]), args[2]);
            case "complete":
                if (args.length < 4) {
                    throw err("Usage: Checkpoint.java complete <target> <module> <completed|skipped> "
                            + "[--gate G] [--reason R] [--compile C] [--compile-attempts N] "
                            + "[--description D] [--report P]");
                }
                return complete(Paths.get(args[1]), args[2], args[3],
                        parseOpts(args, 4, Set.of("gate", "reason", "compile", "compile-attempts",
                                "description", "report"), Set.of()));
            case "reset":
                if (args.length != 3) {
                    throw err("Usage: Checkpoint.java reset <target> <module>");
                }
                return reset(Paths.get(args[1]), args[2]);
            case "resume":
                if (args.length < 2) {
                    throw err("Usage: Checkpoint.java resume <target> [--accept-drift] [--max-resumes N]");
                }
                return resume(Paths.get(args[1]),
                        parseOpts(args, 2, Set.of("max-resumes"), Set.of("accept-drift")));
            default:
                throw err("Unknown command '" + cmd + "'. Valid commands: init status start complete reset resume");
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println(USAGE);
            return;
        }
        Done result;
        try {
            result = run(args);
        } catch (Done d) {
            result = d;
        } catch (IOException | RuntimeException e) {
            result = new Done(1, obj("verdict", "ERROR", "message", String.valueOf(e)));
        }
        System.out.println(Json.stringify(result.payload));
        System.exit(result.code);
    }

    // ------------------------------------------------------------------
    // minimal JSON reader/writer (the JDK ships none)
    // ------------------------------------------------------------------

    static final class Json {
        private final String s;
        private int i;

        private Json(String s) {
            this.s = s;
        }

        /** Parses a JSON document into Map / List / String / Long / Double / Boolean / null. */
        static Object parse(String text) {
            Json j = new Json(text);
            j.ws();
            Object v = j.value();
            j.ws();
            if (j.i != text.length()) {
                throw new IllegalArgumentException("trailing data at " + j.i);
            }
            return v;
        }

        private Object value() {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end of input");
            }
            switch (s.charAt(i)) {
                case '{':
                    return object();
                case '[':
                    return array();
                case '"':
                    return string();
                case 't':
                    expect("true");
                    return Boolean.TRUE;
                case 'f':
                    expect("false");
                    return Boolean.FALSE;
                case 'n':
                    expect("null");
                    return null;
                default:
                    return number();
            }
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            ws();
            if (peek() == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                if (peek() != '"') {
                    throw new IllegalArgumentException("expected string key at " + i);
                }
                String k = string();
                ws();
                consume(':');
                ws();
                m.put(k, value());
                ws();
                char c = next();
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' or '}' at " + (i - 1));
                }
            }
        }

        private List<Object> array() {
            List<Object> l = new ArrayList<>();
            i++; // [
            ws();
            if (peek() == ']') {
                i++;
                return l;
            }
            while (true) {
                ws();
                l.add(value());
                ws();
                char c = next();
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw new IllegalArgumentException("expected ',' or ']' at " + (i - 1));
                }
            }
        }

        private String string() {
            consume('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                char e = next();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 > s.length()) {
                            throw new IllegalArgumentException("bad \\u escape");
                        }
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default:
                        throw new IllegalArgumentException("bad escape \\" + e);
                }
            }
        }

        private Object number() {
            int start = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            if (start == i) {
                throw new IllegalArgumentException("unexpected character '" + s.charAt(i) + "' at " + i);
            }
            String t = s.substring(start, i);
            if (t.matches("-?\\d+")) {
                return Long.parseLong(t);
            }
            return Double.parseDouble(t);
        }

        private void expect(String word) {
            if (!s.startsWith(word, i)) {
                throw new IllegalArgumentException("expected '" + word + "' at " + i);
            }
            i += word.length();
        }

        private void consume(char c) {
            if (next() != c) {
                throw new IllegalArgumentException("expected '" + c + "' at " + (i - 1));
            }
        }

        private char peek() {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end of input");
            }
            return s.charAt(i);
        }

        private char next() {
            if (i >= s.length()) {
                throw new IllegalArgumentException("unexpected end of input");
            }
            return s.charAt(i++);
        }

        private void ws() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        // ---- writer ----

        /** Pretty-prints (2-space indent) a tree of Map / List / String / Number / Boolean / null. */
        static String stringify(Object v) {
            StringBuilder sb = new StringBuilder();
            write(sb, v, 0);
            return sb.toString();
        }

        private static void write(StringBuilder sb, Object v, int depth) {
            if (v == null) {
                sb.append("null");
            } else if (v instanceof String) {
                quote(sb, (String) v);
            } else if (v instanceof Boolean || v instanceof Number) {
                sb.append(v);
            } else if (v instanceof Map) {
                Map<?, ?> m = (Map<?, ?>) v;
                if (m.isEmpty()) {
                    sb.append("{}");
                    return;
                }
                sb.append("{\n");
                int n = 0;
                for (Map.Entry<?, ?> e : m.entrySet()) {
                    indent(sb, depth + 1);
                    quote(sb, String.valueOf(e.getKey()));
                    sb.append(": ");
                    write(sb, e.getValue(), depth + 1);
                    sb.append(++n < m.size() ? ",\n" : "\n");
                }
                indent(sb, depth);
                sb.append('}');
            } else if (v instanceof List) {
                List<?> l = (List<?>) v;
                if (l.isEmpty()) {
                    sb.append("[]");
                    return;
                }
                sb.append("[\n");
                for (int k = 0; k < l.size(); k++) {
                    indent(sb, depth + 1);
                    write(sb, l.get(k), depth + 1);
                    sb.append(k + 1 < l.size() ? ",\n" : "\n");
                }
                indent(sb, depth);
                sb.append(']');
            } else {
                quote(sb, String.valueOf(v));
            }
        }

        private static void indent(StringBuilder sb, int depth) {
            for (int k = 0; k < depth; k++) {
                sb.append("  ");
            }
        }

        private static void quote(StringBuilder sb, String t) {
            sb.append('"');
            for (int k = 0; k < t.length(); k++) {
                char c = t.charAt(k);
                switch (c) {
                    case '"': sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\n': sb.append("\\n"); break;
                    case '\r': sb.append("\\r"); break;
                    case '\t': sb.append("\\t"); break;
                    case '\b': sb.append("\\b"); break;
                    case '\f': sb.append("\\f"); break;
                    default:
                        if (c < 0x20) {
                            sb.append(String.format("\\u%04x", (int) c));
                        } else {
                            sb.append(c);
                        }
                }
            }
            sb.append('"');
        }
    }
}
