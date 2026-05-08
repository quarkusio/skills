package io.quarkus.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.migration.runner.AgentRunner;
import io.quarkus.migration.runner.RunnerRegistry;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite that runs migration skills against test projects and verifies the results.
 *
 * <p>Configuration via system properties:
 * <ul>
 *   <li>{@code ai.model} — model to use (default: vertex-anthropic/claude-sonnet-4-5@20250929)</li>
 *   <li>{@code ai.strategy} — migration strategy: full or compatibility (default: full)</li>
 *   <li>{@code ai.timeout} — timeout in seconds per project (default: 300)</li>
 *   <li>{@code ai.cmd} — path to AI agent binary (default: opencode)</li>
 *   <li>{@code ai.project} — run only this project (default: all)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * # Run all projects with defaults
 * mvn test
 *
 * # Run specific project
 * mvn test -Dai.project=spring-rest-api
 *
 * # Compare models
 * mvn test -Dai.model=vertex-anthropic/claude-sonnet-4-5@20250929
 * mvn test -Dai.model=google/gemini-2.5-pro
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MigrationTest {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final ResultsTracker tracker = ResultsTracker.defaultTracker();
    private static final SkillResolver skillResolver = new SkillResolver(
            skillsDir(), Path.of("target", "skills").toAbsolutePath());

    // -- config from system properties --

    static String aiProvider() {
        return System.getProperty("ai.provider", "google-vertex-anthropic");
    }

    static String aiModel() {
        return System.getProperty("ai.model", "claude-opus-4-6@default");
    }

    /** Display string for the provider/model combination. */
    static String aiModelDisplay() {
        String p = aiProvider();
        String m = aiModel();
        if (!p.isEmpty() && !m.isEmpty()) return p + "/" + m;
        if (!p.isEmpty()) return p + "/(default)";
        if (!m.isEmpty()) return m;
        return "(ai agent default)";
    }

    static String aiStrategy() {
        return System.getProperty("ai.strategy", "full");
    }

    static int aiTimeout() {
        return Integer.parseInt(System.getProperty("ai.timeout", "300"));
    }

    static String aiCmd() {
        return System.getProperty("ai.cmd", "opencode");
    }

    static String aiPrompt() {
        return System.getProperty("ai.prompt", "");
    }

    static String aiProject() {
        return System.getProperty("ai.project", "");
    }

    /** Skill to use: a local name (looked up in skills/) or a GitHub URL. Overrides project.yaml. */
    static String aiSkill() {
        return System.getProperty("ai.skill", "");
    }

    /**
     * Github branch containing part of its name "/" when we get the skill from a URL and
     * the URL also has a subpath (e.g. tree/feature/my-branch/skills/my-skill). Ignored otherwise.
     */
    static String aiSkillBranch() {
        return System.getProperty("ai.skill.branch", "");
    }

    /** Whether to pass --sanitize when exporting opencode sessions. Default: false. */
    static boolean aiSanitize() {
        return Boolean.parseBoolean(System.getProperty("ai.sanitize", "false"));
    }

    // -- discover test projects --

    static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        // If we're in tests/, go up one level
        if (dir.getFileName().toString().equals("tests") && Files.isDirectory(dir.resolve("projects"))) {
            return dir.getParent();
        }
        // If we're at repo root
        if (Files.isDirectory(dir.resolve("skills"))) {
            return dir;
        }
        // Otherwise try parent
        if (dir.getParent() != null && Files.isDirectory(dir.getParent().resolve("skills"))) {
            return dir.getParent();
        }
        return dir;
    }

    static Path projectsDir() {
        // First check if we're running from tests/ dir
        Path testsDir = Path.of("").toAbsolutePath();
        if (Files.isDirectory(testsDir.resolve("projects"))) {
            return testsDir.resolve("projects");
        }
        return repoRoot().resolve("tests").resolve("projects");
    }

    static Path skillsDir() {
        return repoRoot().resolve("skills");
    }

    static Stream<Arguments> migrationProjects() throws IOException {
        Path projects = projectsDir();
        String filter = aiProject();

        try (var dirs = Files.list(projects)) {
            return dirs
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("project.yaml")))
                    .filter(p -> filter.isEmpty() || p.getFileName().toString().equals(filter))
                    .sorted()
                    .map(p -> {
                        try {
                            ProjectConfig config = YAML.readValue(
                                    p.resolve("project.yaml").toFile(),
                                    ProjectConfig.class);
                            return Arguments.of(config, p);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .toList()  // materialize before stream closes
                    .stream();
        }
    }

    // -- the actual test --

    @ParameterizedTest(name = "{0}")
    @MethodSource("migrationProjects")
    @Order(1)
    void migrate(ProjectConfig config, Path projectDir) throws Exception {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PROJECT: " + config.name());
        System.out.println("  provider: " + (aiProvider().isEmpty() ? "(default)" : aiProvider()));
        System.out.println("  model:    " + (aiModel().isEmpty() ? "(default)" : aiModel()));
        System.out.println("  strategy: " + aiStrategy());
        System.out.println("  timeout:  " + aiTimeout() + "s");
        System.out.println("  checks:   " + config.checks());
        System.out.println("=".repeat(60));

        // 1. Prepare working directory and output directory
        Path workDir = prepareWorkDir(config, projectDir);

        // Build a run name: project_provider_model_strategy
        String providerShort = aiProvider().isEmpty() ? "default" : aiProvider().replaceAll("[^a-zA-Z0-9-]", "-");
        String modelShort = aiModel().isEmpty() ? "default" : aiModel().replaceAll("[^a-zA-Z0-9-]", "-");
        String runName = config.name() + "_" + providerShort + "_" + modelShort + "_" + aiStrategy();
        Path outputDir = Path.of("target", "runs").toAbsolutePath();

        System.out.println("  workdir:  " + workDir);
        System.out.println("  outputs:  " + outputDir.resolve(runName + ".*"));

        // 2. Resolve the skill
        String skillRefStr = aiSkill().isEmpty() ? config.skill() : aiSkill();
        Path skillPath = skillResolver.resolve(skillRefStr, aiSkillBranch());

        boolean isUrl = skillRefStr.startsWith("https://") || skillRefStr.startsWith("http://") || skillRefStr.startsWith("git@");
        SkillReference skillRef = new SkillReference(
                config.skill(),
                isUrl ? skillRefStr : null,
                skillPath.toString());

        MigrationResult result = new MigrationResult(aiCmd(),
                config.name(), aiModelDisplay(), aiStrategy(), skillRef);
        result.setWorkDir(workDir.toString());
        result.setRunName(runName);
        assertTrue(Files.isDirectory(skillPath),
                "Skill directory not found: " + skillPath);

        int timeout = config.timeout() > 0 ? config.timeout() : aiTimeout();

        // Select the agent from the Registry
        AgentRunner runner = RunnerRegistry.getRunner(aiCmd(), aiProvider(), aiModel(), skillPath, aiStrategy(), timeout, aiPrompt(), aiSanitize());

        System.out.printf("  Running migration agent: %s ...%n",aiCmd());
        AgentRunner.RunOutput output = runner.run(workDir, outputDir, runName);

        result.setAiExitCode(output.exitCode());
        result.setDuration(output.duration());
        result.setSessionFiles(output.sessionFiles());

        System.out.println("  Migration completed in " + output.duration().toSeconds() + "s (exit=" + output.exitCode() + ")");

        // 3. Extract usage stats from session
        AgentRunner.UsageStats usage = runner.extractUsage(output.sessionFiles());
        result.setTotalTokens(usage.totalTokens());
        result.setTotalCost(usage.totalCost());
        result.setApiCalls(usage.apiCalls());
        result.setReviewTokens(usage.totalTokens());
        result.setReviewCost(usage.totalCost());
        result.setApiCalls(usage.apiCalls());

        /*
        System.out.println("  Tokens: " + usage.totalTokens() +
                "  Cost: $" + String.format("%.4f", usage.totalCost()) +
                "  API calls: " + usage.apiCalls());
         */

        // 4. Run checks
        MigrationChecks checks = new MigrationChecks(workDir);
        System.out.println("  Running checks...");

        List<String> failures = new ArrayList<>();
        Optional.ofNullable(config.checks())
                .ifPresent(list -> list.forEach(check -> {
                    System.out.print("    " + check + " ... ");
                    boolean passed = checks.runCheck(check);
                    result.addCheck(check, passed);
                    System.out.println(passed ? "✅" : "❌");
                    if (!passed) {
                        failures.add(check);
                    }
                }));


        /* TODO: To be reviewed and investigated: Do we need it ?
        // 5. Run skill review (separate ai session)
        AgentRunner.ReviewOutput reviewOutput = runner.review(
                output.sessionFiles().getFirst(), workDir, outputDir, runName, skillPath, result.getChecks());
        result.setReview(reviewOutput.review());
        result.setReviewTokens(reviewOutput.usage().totalTokens());
        result.setReviewCost(reviewOutput.usage().totalCost());
         */

        // 6. Record result
        tracker.record(result);
        System.out.println("\n" + result);

        // 7. Assert all checks passed
        if (!failures.isEmpty()) {
            fail("Migration checks failed: " + failures + "\n" +
                    "Work dir preserved at: " + workDir + "\n" +
                    "Score: " + result.score());
        }
    }

    // -- helpers --

    private Path prepareWorkDir(ProjectConfig config, Path projectDir) throws IOException, InterruptedException {
        // Put work dirs under target/workdirs/ so they survive JVM exit but get cleaned on mvn clean
        Path workdirsBase = Path.of("").toAbsolutePath().resolve("target").resolve("workdirs");
        Path workDir = workdirsBase.resolve(config.name());
        // Clean any previous run
        if (Files.exists(workDir)) {
            try (var walk = Files.walk(workDir)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.delete(p); } catch (IOException ignored) {} });
            }
        }
        Files.createDirectories(workDir);

        if (config.isLocal()) {
            Path source = projectDir.resolve("source");
            assertTrue(Files.isDirectory(source),
                    "Local source directory not found: " + source);
            copyDirectory(source, workDir);
        } else {
            // Clone from git
            List<String> cmd = new ArrayList<>(List.of(
                    "git", "clone", "--depth", "1"));
            if (config.ref() != null && !config.ref().isBlank()) {
                cmd.addAll(List.of("--branch", config.ref()));
            }
            cmd.add(config.source());
            cmd.add(workDir.toString());

            Process p = new ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start();

            boolean done = p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS);
            assertTrue(done && p.exitValue() == 0,
                    "Failed to clone " + config.source());
        }

        return workDir;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        try (var stream = Files.walk(source)) {
            stream.forEach(src -> {
                Path dest = target.resolve(source.relativize(src));
                try {
                    if (Files.isDirectory(src)) {
                        Files.createDirectories(dest);
                    } else {
                        Files.createDirectories(dest.getParent());
                        Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
