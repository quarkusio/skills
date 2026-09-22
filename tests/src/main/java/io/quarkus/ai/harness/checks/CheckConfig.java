package io.quarkus.ai.harness.checks;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record CheckConfig(
        List<EndpointCheck> endpoints,
        Integer retries,
        @JsonProperty("retry_delay_ms") Integer retryDelayMs
) {
    public static final int DEFAULT_RETRIES = 5;
    public static final int DEFAULT_RETRY_DELAY_MS = 2000;

    public CheckConfig {
        if (endpoints == null) endpoints = List.of();
    }

    public int effectiveRetries() {
        return retries != null ? retries : DEFAULT_RETRIES;
    }

    public int effectiveRetryDelayMs() {
        return retryDelayMs != null ? retryDelayMs : DEFAULT_RETRY_DELAY_MS;
    }
}