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
    record ModelUsage(String model, long inputTokens, long outputTokens,
                      long cacheRead, long cacheWrite, double cost) {}
    record UsageStats(long totalTokens, double totalCost, int apiCalls, int toolCalls,
                      String actualModel,
                      long inputTokens, long outputTokens, long cacheRead, long cacheWrite,
                      List<ModelUsage> modelUsages) {
        UsageStats(long totalTokens, double totalCost, int apiCalls, String actualModel) {
            this(totalTokens, totalCost, apiCalls, 0, actualModel, 0, 0, 0, 0, List.of());
        }
        UsageStats(long totalTokens, double totalCost, int apiCalls, int toolCalls, String actualModel) {
            this(totalTokens, totalCost, apiCalls, toolCalls, actualModel, 0, 0, 0, 0, List.of());
        }
        UsageStats(long totalTokens, double totalCost, int apiCalls, String actualModel,
                   long inputTokens, long outputTokens, long cacheRead, long cacheWrite) {
            this(totalTokens, totalCost, apiCalls, 0, actualModel,
                    inputTokens, outputTokens, cacheRead, cacheWrite, List.of());
        }
    }
    record ReviewOutput(String review, UsageStats usage) {}
}
