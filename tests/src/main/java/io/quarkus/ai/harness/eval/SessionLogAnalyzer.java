package io.quarkus.ai.harness.eval;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parses ACP session log files (JSONL) and extracts tool call events.
 *
 * <p>Tool calls appear as {@code session/update} messages with
 * {@code params.update.sessionUpdate = "tool_call"} and
 * {@code params.update.status = "pending"}.
 */
public final class SessionLogAnalyzer {

    private static final ObjectMapper JSON = new ObjectMapper();

    private SessionLogAnalyzer() {}

    public record ToolCallEvent(String name, String toolCallId, String title, String kind, String status) {}

    /**
     * Parses a session log file and returns all tool call initiation events.
     */
    public static List<ToolCallEvent> extractToolCalls(Path sessionLog) throws IOException {
        List<ToolCallEvent> calls = new ArrayList<>();
        int parseErrors = 0;

        for (String line : Files.readAllLines(sessionLog)) {
            if (line.isBlank()) continue;
            try {
                JsonNode node = JSON.readTree(line);
                JsonNode update = node.path("params").path("update");
                String sessionUpdate = update.path("sessionUpdate").asText("");

                if (!"tool_call".equals(sessionUpdate)) continue;
                if (!"pending".equals(update.path("status").asText(""))) continue;

                String name = update.path("name").asText("");
                if (name.isEmpty()) {
                    name = update.path("_meta").path("claudeCode").path("toolName").asText("");
                }

                calls.add(new ToolCallEvent(
                        name,
                        update.path("toolCallId").asText(""),
                        update.path("title").asText(""),
                        update.path("kind").asText(""),
                        update.path("status").asText("")
                ));
            } catch (Exception e) {
                parseErrors++;
            }
        }
        if (parseErrors > 0) {
            System.err.printf("  Warning: %d lines failed to parse in %s%n",
                    parseErrors, sessionLog.getFileName());
        }
        return calls;
    }

    /**
     * Returns tool call counts grouped by tool name.
     */
    public static Map<String, Long> toolCallCounts(List<ToolCallEvent> calls) {
        return calls.stream()
                .collect(Collectors.groupingBy(ToolCallEvent::name, LinkedHashMap::new, Collectors.counting()));
    }

    /**
     * Checks which expected tools were actually called.
     *
     * @param calls         the tool calls extracted from the session log
     * @param expectedTools the list of tool names expected to be called
     * @return a map of tool name to whether it was found (true = called, false = missing)
     */
    public static Map<String, Boolean> checkExpectedTools(List<ToolCallEvent> calls, List<String> expectedTools) {
        Map<String, Long> counts = toolCallCounts(calls);
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String expected : expectedTools) {
            result.put(expected, counts.containsKey(expected));
        }
        return result;
    }
}
