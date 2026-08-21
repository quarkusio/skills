package io.quarkus.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.quarkus.migration.runner.AgentRunner;
import io.quarkus.migration.runner.RunnerRegistry;
import static io.quarkus.migration.runner.RunnerRegistry.resolveModel;
import static io.quarkus.migration.runner.RunnerRegistry.resolveProvider;
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

    /** Display string for the resolved provider/model combination. */
    static String aiModelDisplay(String provider, String model) {
        if (!provider.isEmpty() && !model.isEmpty()) return provider + "/" + model;
        if (!model.isEmpty()) return model;
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

    /** Whether to run verification checks after migration. Default: true. */
    static boolean aiChecks() {
        return Boolean.parseBoolean(System.getProperty("runChecks", "true"));
    }

    /** Whether to run the skill review step after migration. Default: true. */
    static boolean aiReview() {
        return Boolean.parseBoolean(System.getProperty("ai.review", "true"));
    }

    /** Number of times to repeat the migration. Default: 1. */
    static int runs() {
        return Integer.parseInt(System.getProperty("runs", "1"));
    }

    /** Comma-separated list of projects to test. Overrides ai.project when set. */
    static String aiProjects() {
        return System.getProperty("ai.projects", "");
    }

    /** Comma-separated list of skills for benchmark (max 2). Overrides ai.skill when set. */
    static List<String> aiSkills() {
        String val = System.getProperty("ai.skills", "");
        if (val.isEmpty()) return List.of();
        String[] parts = val.split(",");
        if (parts.length > 2) {
            throw new IllegalArgumentException("ai.skills supports at most 2 skills for benchmark, got: " + parts.length);
        }
        return Arrays.stream(parts).map(String::trim).toList();
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
        String multiFilter = aiProjects();
        String singleFilter = aiProject();

        Set<String> filterSet = new LinkedHashSet<>();
        if (!multiFilter.isEmpty()) {
            for (String p : multiFilter.split(",")) {
                filterSet.add(p.trim());
            }
        } else if (!singleFilter.isEmpty()) {
            filterSet.add(singleFilter);
        }

        try (var dirs = Files.list(projects)) {
            return dirs
                    .filter(Files::isDirectory)
                    .filter(p -> Files.exists(p.resolve("project.yaml")))
                    .filter(p -> filterSet.isEmpty() || filterSet.contains(p.getFileName().toString()))
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

    private static final Map<String, List<MigrationResult>> globalResultsBySkill = new LinkedHashMap<>();

    // -- the actual test --

    @ParameterizedTest(name = "{0}")
    @MethodSource("migrationProjects")
    @Order(1)
    void migrate(ProjectConfig config, Path projectDir) throws Exception {
        // Resolve provider/model defaults per agent
        String provider = resolveProvider(aiCmd(), aiProvider());
        String model = resolveModel(aiCmd(), aiModel());
        String modelDisplay = aiModelDisplay(provider, model);

        boolean projectDefinesChecks = config.checks() != null && !config.checks().isEmpty();
        boolean hasChecks = aiChecks() && projectDefinesChecks;
        int totalRuns = runs();

        // Determine which skills to iterate
        List<String> skills = aiSkills();
        boolean isBenchmark = skills.size() > 1;
        if (skills.isEmpty()) {
            String singleSkill = aiSkill().isEmpty() ? config.skill() : aiSkill();
            skills = List.of(singleSkill);
        }

        System.out.println("\n" + "=".repeat(60));
        System.out.println("PROJECT: " + config.name());
        System.out.println("  agent:    " + aiCmd());
        System.out.println("  provider: " + (provider.isEmpty() ? "(n/a)" : provider));
        System.out.println("  model:    " + model);
        System.out.println("  timeout:  " + aiTimeout() + "s");
        System.out.println("  checks:   " + (hasChecks ? config.checks() : !aiChecks() ? "disabled (runChecks=false)" : "disabled (none defined)"));
        System.out.println("  skills:   " + skills);
        if (totalRuns > 1) {
            System.out.println("  runs:     " + totalRuns + " (per skill)");
        }
        System.out.println("=".repeat(60));

        Path outputDir = Path.of("target", "runs").toAbsolutePath();
        int timeout = config.timeout() > 0 ? config.timeout() : aiTimeout();
        String modelShort = model.isEmpty() ? "default" : model.replaceAll("[^a-zA-Z0-9-]", "-");

        List<String> lastFailures = new ArrayList<>();
        Path lastWorkDir = null;
        String lastScore = "0/0";

        for (String skillRefStr : skills) {
            Path skillPath = skillResolver.resolve(skillRefStr, aiSkillBranch());

            boolean isUrl = skillRefStr.startsWith("https://") || skillRefStr.startsWith("http://") || skillRefStr.startsWith("git@");
            SkillReference skillRef = new SkillReference(
                    isUrl ? extractSkillShortName(skillRefStr) : skillRefStr,
                    isUrl ? skillRefStr : null,
                    skillPath.toString());

            assertTrue(Files.isDirectory(skillPath),
                    "Skill directory not found: " + skillPath);

            String skillShort = extractSkillShortName(skillRefStr);
            boolean isSpringMigration = "spring-boot".equals(config.type()) && isMigrationSkill(skillRefStr);
            if (isSpringMigration) {
                System.out.println("  strategy: " + aiStrategy());
            }
            String suffix = isSpringMigration ? "_" + modelShort + "_" + aiStrategy() : "_" + modelShort;
            String baseRunName = isBenchmark
                    ? config.name() + "_" + skillShort + suffix
                    : config.name() + suffix;

            if (isBenchmark) {
                System.out.println("\n" + "=".repeat(60));
                System.out.printf("  SKILL: %s%n", skillRefStr);
                System.out.println("=".repeat(60));
            }

            List<MigrationResult> skillResults = new ArrayList<>();

            for (int run = 1; run <= totalRuns; run++) {
                String runName = totalRuns > 1 ? baseRunName + "_run" + run : baseRunName;

                if (totalRuns > 1) {
                    System.out.println("\n" + "-".repeat(60));
                    System.out.printf("  RUN %d/%d%n", run, totalRuns);
                    System.out.println("-".repeat(60));
                }

                // 1. Prepare a fresh working directory
                Path workDir = prepareWorkDir(config, projectDir);
                lastWorkDir = workDir;

                System.out.println("  workdir:  " + workDir);
                System.out.println("  outputs:  " + outputDir.resolve(runName + ".*"));

                MigrationResult result = new MigrationResult(aiCmd(),
                        config.name(), modelDisplay, aiStrategy(), skillRef);
                result.setWorkDir(workDir.toString());
                result.setRunName(runName);
                result.setPrompt(aiPrompt());
                result.setUserProvider(aiProvider());
                result.setUserModel(aiModel());
                result.setProjectType(config.type());

                // 2. Run migration
                AgentRunner runner = RunnerRegistry.getRunner(aiCmd(), provider, model, skillPath, aiStrategy(), timeout, aiPrompt(), aiSanitize());

                System.out.printf("  Running migration agent: %s ...%n", aiCmd());
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
                result.setToolCalls(usage.toolCalls());
                result.setInputTokens(usage.inputTokens());
                result.setOutputTokens(usage.outputTokens());
                result.setCacheRead(usage.cacheRead());
                result.setCacheWrite(usage.cacheWrite());
                result.setModelUsages(usage.modelUsages());

                // 4. Run checks
                List<String> failures = new ArrayList<>();
                if (hasChecks) {
                    MigrationChecks checks = new MigrationChecks(workDir);
                    System.out.println("  Running checks...");

                    config.checks().forEach(check -> {
                        System.out.print("    " + check + " ... ");
                        boolean passed = checks.runCheck(check);
                        result.addCheck(check, passed);
                        System.out.println(passed ? "PASS" : "FAIL");
                        if (!passed) {
                            failures.add(check);
                        }
                    });
                } else {
                    System.out.println("  Skipping checks" + (!aiChecks() ? " (runChecks=false)" : " (none defined)"));
                }

                // 5. Run skill review (separate ai session)
                if (aiReview() && hasChecks && !output.sessionFiles().isEmpty()) {
                    AgentRunner.ReviewOutput reviewOutput = runner.review(
                            output.sessionFiles().getFirst(), workDir, outputDir, runName, skillPath, result.getChecks());
                    result.setReview(reviewOutput.review());
                    result.setReviewTokens(reviewOutput.usage().totalTokens());
                    result.setReviewCost(reviewOutput.usage().totalCost());
                } else {
                    String reason = !aiReview() ? " (ai.review=false)"
                            : output.sessionFiles().isEmpty() ? " (no session files exported)"
                            : " (no checks defined)";
                    System.out.println("  Skipping skill review" + reason);
                }

                // 6. Record result
                tracker.record(result);
                skillResults.add(result);
                System.out.println("\n" + result);

                lastFailures = failures;
                lastScore = result.score();
            }

            // 7. Write per-skill summary when multiple runs
            if (totalRuns > 1) {
                tracker.writeSummaryReport(skillResults, baseRunName);
            }

            // Collect for global summary
            globalResultsBySkill.computeIfAbsent(skillRefStr, k -> new ArrayList<>()).addAll(skillResults);
        }

        // 8. Assert last run's checks passed (skip in benchmark mode to collect all results)
        if (!lastFailures.isEmpty() && !isBenchmark) {
            fail("Migration checks failed: " + lastFailures + "\n" +
                    "Work dir preserved at: " + lastWorkDir + "\n" +
                    "Score: " + lastScore);
        }
    }

    @AfterAll
    static void generateGlobalSummary() {
        boolean multipleSkills = globalResultsBySkill.size() > 1;
        boolean multipleProjects = globalResultsBySkill.values().stream()
                .flatMap(List::stream)
                .map(MigrationResult::getProject)
                .distinct()
                .count() > 1;

        if (multipleSkills || multipleProjects) {
            tracker.writeGlobalSummary(globalResultsBySkill);
        }
    }

    private static String extractSkillShortName(String skillRef) {
        String name = skillRef;
        if (skillRef.contains("/")) {
            name = skillRef.substring(skillRef.lastIndexOf('/') + 1);
        }
        return name.replaceAll("[^a-zA-Z0-9-]", "-");
    }

    private static boolean isMigrationSkill(String skillRef) {
        String name = extractSkillShortName(skillRef).toLowerCase();
        return name.contains("migrate");
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
