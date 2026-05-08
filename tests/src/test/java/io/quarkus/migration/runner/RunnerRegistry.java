package io.quarkus.migration.runner;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum RunnerRegistry {
    PI("pi", PiRunner::new),
    OPENCODE("opencode", OpenCodeRunner::new);

    @FunctionalInterface
    interface RunnerFactory {
        AgentRunner create(String aiCmd, String provider, String model,
                           Path skillPath, String strategy, int timeoutSeconds, String prompt, boolean sanitize);
    }

    private final String key;
    private final RunnerFactory factory;

    // Fast lookup cache map
    private static final Map<String, RunnerRegistry> LOOKUP_MAP = new HashMap<>();

    static {
        for (RunnerRegistry registry : values()) {
            LOOKUP_MAP.put(registry.key.toLowerCase(), registry);
        }
    }

    RunnerRegistry(String key, RunnerFactory factory) {
        this.key = key;
        this.factory = factory;
    }

    // Factory method
    public static AgentRunner getRunner(String aiCmd, String provider, String model,
                                        Path skillPath, String strategy, int timeoutSeconds, String prompt, boolean sanitize) {
        if (aiCmd == null) {
            throw new IllegalArgumentException("aiCmd cannot be null");
        }
        RunnerRegistry registry = LOOKUP_MAP.get(aiCmd.toLowerCase().trim());
        if (registry == null) {
            throw new IllegalArgumentException("No runner registered for the agent: " + aiCmd);
        }
        return registry.factory.create(aiCmd, provider, model, skillPath, strategy, timeoutSeconds, prompt, sanitize);
    }
}