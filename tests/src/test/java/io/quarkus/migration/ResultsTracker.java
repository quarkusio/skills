package io.quarkus.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.*;
import java.time.Instant;

/**
 * Appends migration results to a JSONL history file for trend tracking.
 */
public class ResultsTracker {

    private static final ObjectMapper JSON = new ObjectMapper()
            .disable(SerializationFeature.INDENT_OUTPUT);

    private final Path historyFile;

    public ResultsTracker(Path historyFile) {
        this.historyFile = historyFile;
    }

    /**
     * Default history file location: target/runs/history.jsonl
     */
    public static ResultsTracker defaultTracker() {
        Path runsDir = Path.of("target", "runs").toAbsolutePath();
        try {
            Files.createDirectories(runsDir);
        } catch (IOException ignored) {
        }
        return new ResultsTracker(runsDir.resolve("history.jsonl"));
    }

    /**
     * Append a result to the history file.
     */
    public void record(MigrationResult result) {
        ObjectNode node = JSON.createObjectNode();
        node.put("project", result.getProject());
        node.put("date", Instant.now().toString());
        node.put("model", result.getModel());
        node.put("strategy", result.getStrategy());
        node.put("skill", result.getSkill());
        SkillReference ref = result.getSkillRef();
        ObjectNode skillNode = node.putObject("skill_ref");
        skillNode.put("name", ref.name());
        if (ref.url() != null) {
            skillNode.put("url", ref.url());
        }
        skillNode.put("local_path", ref.localPath());
        node.put("duration_seconds", result.getDuration().toSeconds());

        ObjectNode usage = node.putObject("usage");
        usage.put("total_tokens", result.getTotalTokens());
        usage.put("total_cost", result.getTotalCost());
        usage.put("api_calls", result.getApiCalls());

        ObjectNode checks = node.putObject("checks");
        result.getChecks().forEach(checks::put);

        node.put("score", result.score());
        node.put("ai_exit_code", result.getAiExitCode());
        node.put("run_name", result.getRunName());
        node.put("workdir", result.getWorkDir());

        if (result.getReview() != null && !result.getReview().isBlank()) {
            ObjectNode review = node.putObject("review");
            review.put("tokens", result.getReviewTokens());
            review.put("cost", result.getReviewCost());
            // Store first 500 chars as summary; full review is in .migration-review.md
            String reviewSummary = result.getReview();
            if (reviewSummary.length() > 500) {
                reviewSummary = reviewSummary.substring(0, 500) + "...";
            }
            review.put("summary", reviewSummary);
        }

        try {
            Files.writeString(historyFile,
                    JSON.writeValueAsString(node) + "\n",
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write result to history: " + e.getMessage());
        }
    }

    public Path getHistoryFile() {
        return historyFile;
    }
}
