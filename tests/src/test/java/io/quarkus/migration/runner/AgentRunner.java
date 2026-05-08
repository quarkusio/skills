package io.quarkus.migration.runner;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface AgentRunner {

    RunOutput run(Path projectDir, Path outputDir, String runName) throws IOException, InterruptedException;
    UsageStats extractUsage(List<String> sessionFiles);

    ReviewOutput review(String sessionFile, Path projectDir, Path outputDir,
                        String runName, Path skillPath,
                        Map<String, Boolean> checkResults) throws IOException, InterruptedException;

    record RunOutput(int exitCode, Duration duration, List<String> sessionFiles, String logFile) {}
    record UsageStats(long totalTokens, double totalCost, int apiCalls, String actualModel) {}
    record ReviewOutput(String review, UsageStats usage) {}
}
