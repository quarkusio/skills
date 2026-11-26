package io.quarkus.ai.harness;

import io.quarkus.ai.harness.eval.ExecutionFidelityConfig;
import io.quarkus.ai.harness.eval.ExecutionFidelityConfig.ExpectedExecution;
import io.quarkus.ai.harness.eval.SessionLogAnalyzer;
import io.quarkus.ai.harness.eval.SessionLogAnalyzer.ToolCallEvent;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Analyzes session logs from prior migration runs to verify that expected
 * tool calls were attempted during execution.
 *
 * <p>This test does NOT launch a new migration. It reads session log files
 * from {@code target/runs/} (or a directory specified via {@code eval.session.dir})
 * and checks them against the expected tool call configuration.
 *
 * <p>Usage:
 * <pre>
 * # Run after a migration to verify execution fidelity
 * mvn test -Peval-fidelity
 *
 * # Point to a specific session log directory
 * mvn test -Peval-fidelity -Deval.session.dir=target/runs
 *
 * # Check a single session log file
 * mvn test -Peval-fidelity -Deval.session.file=target/runs/spring-jpa-crud_default_full.json
 * </pre>
 */
class ExecutionFidelityTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @TestFactory
    Collection<DynamicTest> verifyToolCallFidelity() throws Exception {
        ExecutionFidelityConfig config = loadConfig();
        List<Path> sessionLogs = findSessionLogs();

        if (sessionLogs.isEmpty()) {
            return List.of(DynamicTest.dynamicTest(
                    "No session logs found",
                    () -> fail("No session log files found. Run a migration first with: " +
                            "mvn test -Pintegration -Dai.projects=spring-jpa-crud")));
        }

        List<DynamicTest> tests = new ArrayList<>();

        for (Path logFile : sessionLogs) {
            String logName = logFile.getFileName().toString();
            List<ToolCallEvent> calls = SessionLogAnalyzer.extractToolCalls(logFile);

            for (ExpectedExecution exec : config.executions()) {
                PathMatcher matcher = FileSystems.getDefault()
                        .getPathMatcher("glob:" + exec.sessionLogPattern());
                if (!matcher.matches(logFile.getFileName())) continue;

                boolean required = exec.isRequired();
                String testName = logName + " / " + exec.id()
                        + (required ? "" : " [advisory]");

                tests.add(DynamicTest.dynamicTest(testName, () -> {
                    System.out.printf("  Session: %s (%d tool calls)%n", logName, calls.size());

                    Map<String, Long> counts = SessionLogAnalyzer.toolCallCounts(calls);
                    System.out.println("  Tool call summary:");
                    counts.forEach((tool, count) ->
                            System.out.printf("    %-20s %d%n", tool, count));

                    Map<String, Boolean> presence = SessionLogAnalyzer.checkExpectedTools(
                            calls, exec.expectedTools());
                    List<String> missing = presence.entrySet().stream()
                            .filter(e -> !e.getValue())
                            .map(Map.Entry::getKey)
                            .toList();

                    List<String> belowMin = new ArrayList<>();
                    if (exec.minCalls() != null) {
                        for (var min : exec.minCalls()) {
                            long actual = counts.getOrDefault(min.tool(), 0L);
                            if (actual < min.min()) {
                                belowMin.add("%s: expected >= %d, got %d"
                                        .formatted(min.tool(), min.min(), actual));
                            }
                        }
                    }

                    boolean passed = missing.isEmpty() && belowMin.isEmpty();
                    System.out.println();
                    System.out.printf("  Expected tools: %s%n", exec.expectedTools());
                    if (passed) {
                        System.out.println("  Result: ALL expected tools were called");
                    } else {
                        String label = required ? "FAIL" : "ADVISORY";
                        if (!missing.isEmpty())
                            System.out.printf("  [%s] MISSING tools: %s%n", label, missing);
                        if (!belowMin.isEmpty())
                            System.out.printf("  [%s] Below minimum: %s%n", label, belowMin);
                    }

                    if (required) {
                        assertTrue(passed,
                                "Execution fidelity check '%s' failed for %s. Missing: %s. Below min: %s"
                                        .formatted(exec.id(), logName, missing, belowMin));
                    }
                }));
            }
        }

        if (tests.isEmpty()) {
            tests.add(DynamicTest.dynamicTest(
                    "No matching checks",
                    () -> System.out.println("No execution fidelity checks matched the available session logs")));
        }

        return tests;
    }

    private ExecutionFidelityConfig loadConfig() throws Exception {
        String testFile = System.getProperty("eval.fidelity.tests",
                "evals/execution-fidelity-tests.json");

        InputStream is = getClass().getClassLoader().getResourceAsStream(testFile);
        if (is != null) {
            return JSON.readValue(is, ExecutionFidelityConfig.class);
        }

        Path filePath = Path.of(testFile);
        if (Files.exists(filePath)) {
            return JSON.readValue(filePath.toFile(), ExecutionFidelityConfig.class);
        }

        throw new IllegalStateException(
                "Fidelity test config not found: " + testFile + " (tried classpath and filesystem)");
    }

    private List<Path> findSessionLogs() throws Exception {
        String singleFile = System.getProperty("eval.session.file", "");
        if (!singleFile.isEmpty()) {
            Path p = Path.of(singleFile);
            return Files.exists(p) ? List.of(p) : List.of();
        }

        String sessionDir = System.getProperty("eval.session.dir", "target/runs");
        Path dir = Path.of(sessionDir).toAbsolutePath();
        if (!Files.isDirectory(dir)) return List.of();

        try (Stream<Path> files = Files.list(dir)) {
            return files
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.toString().endsWith(".review.json"))
                    .sorted()
                    .toList();
        }
    }
}
