package io.quarkus.ai.harness.eval;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TriggerEvalConfig(
        String skill,
        List<TestCase> tests
) {
    public record TestCase(
            String id,
            String prompt,
            @JsonProperty("should_trigger") boolean shouldTrigger,
            String category
    ) {}
}
