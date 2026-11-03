package io.quarkus.ai.harness.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Configuration for execution fidelity tests.
 *
 * <p>Each entry maps a session log pattern to the list of tool calls
 * that should have been attempted during execution.
 */
public record ExecutionFidelityConfig(
        String skill,
        List<ExpectedExecution> executions
) {
    public record ExpectedExecution(
            String id,
            String description,
            @JsonProperty("session_log_pattern") String sessionLogPattern,
            @JsonProperty("expected_tools") List<String> expectedTools,
            @JsonProperty("min_calls") List<MinToolCalls> minCalls,
            @JsonProperty("required") Boolean required
    ) {
        public boolean isRequired() {
            return required == null || required;
        }
    }

    public record MinToolCalls(
            String tool,
            int min
    ) {}
}
