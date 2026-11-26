package io.quarkus.ai.harness.eval;

import io.smallrye.agentclientprotocol.sdk.client.AcpClient;
import io.smallrye.agentclientprotocol.sdk.client.AcpSyncClient;
import io.smallrye.agentclientprotocol.sdk.client.transport.AgentParameters;
import io.smallrye.agentclientprotocol.sdk.client.transport.StdioAcpClientTransport;
import io.smallrye.agentclientprotocol.sdk.registry.AcpRegistryManager;
import io.smallrye.agentclientprotocol.sdk.registry.model.InstalledAgent;
import io.smallrye.agentclientprotocol.sdk.spec.schema.v1.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sends classification prompts to an ACP agent to evaluate whether a skill
 * description matches a user prompt (LLM-as-judge).
 *
 * <p>Opens one ACP session and sends all evaluation prompts sequentially,
 * then closes the session. Each evaluation is self-contained in the prompt.
 */
public class TriggerJudge implements AutoCloseable {

    private static final String PERMISSION_ALLOW_ALWAYS = "allow_always";
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String aiAgent;
    private final String model;
    private final String skillName;
    private final String skillDescription;

    private AcpSyncClient client;
    private String sessionId;
    private Path workspace;
    private final StringBuffer responseBuffer = new StringBuffer();

    public TriggerJudge(String aiAgent, String model, String skillName, String skillDescription) {
        this.aiAgent = aiAgent;
        this.model = model;
        this.skillName = skillName;
        this.skillDescription = skillDescription;
    }

    /**
     * Opens the ACP connection: launches the agent process, initializes, and creates a session.
     */
    public void open() throws IOException {
        AcpRegistryManager registry = new AcpRegistryManager();
        InstalledAgent agent = registry.getInstalledAgent(aiAgent);
        if (agent == null) {
            throw new IllegalStateException(
                    "ACP agent '%s' not found under ~/.acp/agents. Install it first.".formatted(aiAgent));
        }

        var params = AgentParameters.builder(agent.cmd());
        for (String arg : agent.args()) {
            String trimmed = arg.trim();
            if (!trimmed.isEmpty()) params.arg(trimmed);
        }

        var transport = new StdioAcpClientTransport(params.build());

        client = AcpClient.sync(transport)
                .withRequestTimeout(Duration.ofSeconds(30))
                .withPromptRequestTimeout(Duration.ofSeconds(120))
                .withNotifications(n -> n.onAgentMessage(chunk -> {
                    String text = extractText(chunk.content());
                    if (!text.isEmpty()) {
                        responseBuffer.append(text);
                    }
                }))
                .withPermissionMode(PERMISSION_ALLOW_ALWAYS)
                .build();

        client.initialize();

        this.workspace = Files.createTempDirectory("trigger-eval-");
        var session = client.newSession(new NewSessionRequest(workspace.toString(), List.of()));
        sessionId = session.sessionId();

        if (session.configOptions() != null && !model.isEmpty()) {
            resolveAndSetModel(session.configOptions());
        }

        System.out.println("  TriggerJudge: session opened (agent=" + aiAgent + ")");
    }

    /**
     * Evaluates a single test case: sends a judge prompt and parses the response.
     */
    public TriggerEvalResult evaluate(TriggerEvalConfig.TestCase testCase) {
        responseBuffer.setLength(0);

        String prompt = buildJudgePrompt(testCase.prompt());
        client.prompt(new PromptRequest(List.of(new TextContent(prompt)), sessionId));

        String response = responseBuffer.toString().trim();
        return parseResponse(testCase, response);
    }

    /**
     * Evaluates all test cases sequentially and returns results.
     */
    public List<TriggerEvalResult> evaluateAll(List<TriggerEvalConfig.TestCase> testCases) {
        List<TriggerEvalResult> results = new ArrayList<>();
        for (int i = 0; i < testCases.size(); i++) {
            TriggerEvalConfig.TestCase tc = testCases.get(i);
            System.out.printf("  [%d/%d] %s: \"%s\" ... ",
                    i + 1, testCases.size(), tc.id(), truncate(tc.prompt(), 60));
            try {
                TriggerEvalResult result = evaluate(tc);
                System.out.printf("%s (trigger=%b, confidence=%.2f)%n",
                        result.correct() ? "PASS" : "FAIL", result.actualTrigger(), result.confidence());
                results.add(result);
            } catch (Exception e) {
                System.out.printf("ERROR: %s%n", e.getMessage());
                results.add(new TriggerEvalResult(
                        tc.id(), tc.prompt(), tc.shouldTrigger(),
                        !tc.shouldTrigger(), 0.0,
                        "Error: " + e.getMessage(), tc.category()));
            }
        }
        return results;
    }

    @Override
    public void close() {
        if (client != null) {
            try {
                if (sessionId != null) {
                    client.closeSession(new CloseSessionRequest(sessionId));
                }
            } catch (Exception e) {
                System.err.println("  Warning: failed to close session: " + e.getMessage());
            }
            try {
                client.close();
            } catch (Exception e) {
                System.err.println("  Warning: failed to close client: " + e.getMessage());
            }
        }
        if (workspace != null) {
            try {
                Files.deleteIfExists(workspace);
            } catch (Exception e) {
                System.err.println("  Warning: failed to delete temp dir: " + e.getMessage());
            }
        }
    }

    private String buildJudgePrompt(String userPrompt) {
        return """
                You are evaluating whether an AI skill should activate for a given user prompt. \
                Do NOT execute the skill or perform the task described in the prompt. \
                Only evaluate whether the skill description matches the user's intent.

                SKILL NAME: %s
                SKILL DESCRIPTION: %s

                USER PROMPT: %s

                Based on the skill description, should this skill activate for this user prompt?
                Respond ONLY with a JSON object, no other text:
                {"should_trigger": true or false, "confidence": 0.0 to 1.0, "reasoning": "one sentence explaining why"}
                """.formatted(skillName, skillDescription, userPrompt);
    }

    private TriggerEvalResult parseResponse(TriggerEvalConfig.TestCase testCase, String response) {
        try {
            String jsonStr = extractJson(response);
            JsonNode node = JSON.readTree(jsonStr);

            boolean shouldTrigger = node.path("should_trigger").asBoolean(false);
            double confidence = node.path("confidence").asDouble(0.0);
            String reasoning = node.path("reasoning").asText("no reasoning provided");

            return new TriggerEvalResult(
                    testCase.id(), testCase.prompt(), testCase.shouldTrigger(),
                    shouldTrigger, confidence, reasoning, testCase.category());
        } catch (Exception e) {
            boolean fallback = response.toLowerCase().contains("\"should_trigger\": true")
                    || response.toLowerCase().contains("\"should_trigger\":true");
            return new TriggerEvalResult(
                    testCase.id(), testCase.prompt(), testCase.shouldTrigger(),
                    fallback, 0.0,
                    "Failed to parse response: " + e.getMessage() + " | Raw: " + truncate(response, 200),
                    testCase.category());
        }
    }

    private static String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private static String extractText(Object content) {
        if (content instanceof Map<?, ?> map) {
            Object text = map.get("text");
            return text != null ? text.toString() : content.toString();
        }
        return content != null ? content.toString() : "";
    }

    private static String truncate(String s, int maxLen) {
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    private void resolveAndSetModel(List<SessionConfigOption> configOptions) {
        List<String> available = new ArrayList<>();
        for (SessionConfigOption cfg : configOptions) {
            if (("model".equalsIgnoreCase(String.valueOf(cfg.category()))
                    || "model".equalsIgnoreCase(cfg.id())) && cfg.options() != null) {
                for (var opt : cfg.options()) {
                    available.add(opt.value());
                    if (opt.value().equalsIgnoreCase(model)) {
                        setModel(opt.value());
                        return;
                    }
                }
            }
        }
        String modelLower = model.toLowerCase();
        for (String v : available) {
            if (v.toLowerCase().contains(modelLower)) {
                setModel(v);
                return;
            }
        }
        System.err.printf("  Warning: model '%s' not found in agent options. Using agent default.%n", model);
    }

    private void setModel(String resolvedModel) {
        try {
            client.setConfigOption(new SetSessionConfigOptionRequest("model", sessionId, resolvedModel));
            System.out.println("  TriggerJudge: model set to " + resolvedModel);
        } catch (RuntimeException e) {
            System.err.printf("  Warning: agent does not support model override, using default%n");
        }
    }
}
