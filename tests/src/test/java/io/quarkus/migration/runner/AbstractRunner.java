package io.quarkus.migration.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;

public abstract class AbstractRunner {
    protected String aiCmd;
    protected String provider;
    protected String model;
    protected String strategy;
    protected int timeoutSeconds;
    protected Path skillPath;
    protected String prompt;
    protected boolean sanitize;
    protected ObjectMapper JSON;

    public AbstractRunner(String aiCmd, String provider, String model, Path skillPath, String strategy, int timeoutSeconds,
            String prompt, boolean sanitize) {
        this.aiCmd = aiCmd;
        this.provider = provider;
        this.model = model;
        this.skillPath = skillPath;
        this.strategy = strategy;
        this.timeoutSeconds = timeoutSeconds;
        this.prompt = prompt;
        this.sanitize = sanitize;
        this.JSON = new ObjectMapper();
    }

    /**
     * Add the model args to the command.
     *
     * @param cmd The Ai command to be enriched with the provider/model
     */
    abstract void addModelArgs(List<String> cmd);

    /**
     * Parse ai session JSONL files to extract token usage and cost.
     */
    public abstract AgentRunner.UsageStats extractUsage(List<String> sessionFiles);

    public void writeOpenCodeConfiguration() {
        // 1. Define the target file path relative to where the program is running
        Path cfgPath = Paths.get("opencode.json");

        // 2. Define the exact JSON string payload
        // Note: We escape the internal double quotes (\") and use \n for the newline
        String jsonPayload = "{\"$schema\": \"https://opencode.ai/config.json\",\"permission\": \"allow\"}\n";

        try {
            // 3. Write the string payload to the file
            // CREATE: creates the file if it doesn't exist
            // TRUNCATE_EXISTING: if it exists, overwrites it completely (matching the '>' behavior)
            Files.writeString(
                    cfgPath,
                    jsonPayload,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            System.out.println("Successfully wrote opencode config to: " + cfgPath.toAbsolutePath());

        } catch (IOException e) {
            System.err.println("Failed to write the OpenCode configuration file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void copySkills(Path source, Path target) throws IOException {
        // Use try-with-resources to auto-close the stream
        try (var stream = Files.walk(source)) {
            stream.forEach(sourcePath -> {
                try {
                    // Resolve target path relative to source
                    Path targetPath = target.resolve(source.relativize(sourcePath));

                    if (Files.isDirectory(sourcePath)) {
                        // Create directories (including parents)
                        Files.createDirectories(targetPath);
                    } else {
                        // Copy file with attributes and overwrite existing
                        Files.copy(sourcePath, targetPath,
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e); // Handle or propagate
                }
            });
        }
    }

    protected String generateMigrationPrompt() {
        return """
                Migrate this Spring Boot project to Quarkus using the %s migration strategy. \\
                Work entirely within this directory. \\
                Do a full migration — convert all source files, build files, config, and tests. \\
                After migration, verify the project compiles with ./mvnw compile and fix any errors. \\
                Then run ./mvnw test and fix any test failures.
                If you need to delete code or files, explain why you are deleting them and what you are replacing them with.
                If anything could not be converted/migrated explain why - do not just delete/remove it without explaining.
                Include a summary of the migration in the end of the output.""".formatted(strategy);
    }

    /**
     * Print a human-readable summary of a JSON streaming event. Focuses on high-level actions: tool calls, tool results, text
     * output, turns.
     */
    protected void printEvent(JsonNode event, BufferedWriter prettyWriter) {
        String type = event.path("type").asText("");

        switch (type) {
            case "turn_start" -> printBoth("  ┌── turn", prettyWriter);

            case "turn_end" -> printBoth("  └── turn end", prettyWriter);

            case "message_start" -> {
                String role = event.path("message").path("role").asText("");
                if ("assistant".equals(role)) {
                    printBoth("  │ 🤖 assistant:", prettyWriter);
                }
            }

            case "message_end" -> {
                JsonNode msg = event.path("message");
                if ("assistant".equals(msg.path("role").asText(""))) {
                    JsonNode usage = msg.path("usage");
                    long tokens = usage.path("totalTokens").asLong(0);
                    double cost = usage.path("cost").path("total").asDouble(0);
                    if (tokens > 0) {
                        printBoth(String.format("  │    [tokens: %d, cost: $%.4f]", tokens, cost), prettyWriter);
                    }
                }
            }

            case "message_update" -> {
                JsonNode ae = event.path("assistantMessageEvent");
                String aeType = ae.path("type").asText("");

                switch (aeType) {
                    case "text_delta" -> printBothRaw(ae.path("delta").asText(""), prettyWriter);
                    case "text_end" -> printBoth("", prettyWriter);
                    default -> {
                    }
                }
            }

            case "tool_execution_start" -> {
                String toolName = event.path("toolName").asText("");
                JsonNode args = event.path("args");

                String line = switch (toolName) {
                    case "bash" -> {
                        String command = args.path("command").asText("");
                        if (command.length() > 120)
                            command = command.substring(0, 117) + "...";
                        yield "  │ 🔧 bash: " + command;
                    }
                    case "edit" -> {
                        String path = args.path("path").asText("");
                        int edits = args.path("edits").size();
                        yield "  │ 🔧 edit: " + path + " (" + edits + " edit" + (edits != 1 ? "s" : "") + ")";
                    }
                    case "write" -> "  │ 🔧 write: " + args.path("path").asText("");
                    case "read" -> "  │ 🔧 read: " + args.path("path").asText("");
                    default -> "  │ 🔧 " + toolName;
                };
                printBoth(line, prettyWriter);
            }

            case "tool_execution_end" -> {
                JsonNode result = event.path("result");
                if (result.path("isError").asBoolean(false)) {
                    printBoth("  │ ❌ " + event.path("toolName").asText("") + " error", prettyWriter);
                }
            }

            // Ignore: session, agent_start, thinking_*, etc.
        }
    }

    /** Print to both System.out and the pretty log file. */
    protected void printBoth(String text, BufferedWriter prettyWriter) {
        System.out.println(text);
        try {
            synchronized (prettyWriter) {
                prettyWriter.write(text);
                prettyWriter.newLine();
                prettyWriter.flush();
            }
        } catch (IOException ignored) {
        }
    }

    /** Print (no newline) to both System.out and the pretty log file. */
    protected void printBothRaw(String text, BufferedWriter prettyWriter) {
        System.out.print(text);
        System.out.flush();
        try {
            synchronized (prettyWriter) {
                prettyWriter.write(text);
                prettyWriter.flush();
            }
        } catch (IOException ignored) {
        }
    }
}
