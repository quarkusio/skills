package io.quarkus.migration;

import java.util.List;

/**
 * Maps to a project.yaml file defining a test project.
 */
public record ProjectConfig(
        String name,
        String description,
        String type,
        String skill,
        String source,
        String ref,
        int timeout,
        List<String> checks
) {
    public boolean isLocal() {
        return "local".equals(source);
    }
}
