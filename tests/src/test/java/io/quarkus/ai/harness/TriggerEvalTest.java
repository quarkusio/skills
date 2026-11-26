package io.quarkus.ai.harness;

import io.quarkus.ai.harness.eval.TriggerEvalConfig;
import io.quarkus.ai.harness.eval.TriggerEvalReport;
import io.quarkus.ai.harness.eval.TriggerEvalResult;
import io.quarkus.ai.harness.eval.TriggerJudge;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static io.quarkus.ai.harness.config.AiConfig.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Evaluates trigger accuracy of a skill description using an LLM-as-judge approach.
 *
 * <p>Loads test cases from {@code evals/trigger-tests.json}, sends each prompt
 * to an ACP agent for classification, and reports accuracy metrics.
 *
 * <p>Usage:
 * <pre>
 * mvn test -Peval -Dai.agent=claude-acp
 * mvn test -Peval -Dai.agent=claude-acp -Dai.model=claude-sonnet-4-5-20250514
 * </pre>
 */
class TriggerEvalTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final YAMLMapper YAML = new YAMLMapper();

    @Test
    void evaluateTriggerAccuracy() throws Exception {
        TriggerEvalConfig config = loadTestConfig();
        String skillDescription = loadSkillDescription(config.skill());

        String agent = aiAgent();
        String model = aiModel();

        System.out.println("=".repeat(60));
        System.out.println("  Trigger Eval Test");
        System.out.printf("  Skill: %s%n", config.skill());
        System.out.printf("  Agent: %s%n", agent);
        System.out.printf("  Model: %s%n", model.isEmpty() ? "(agent default)" : model);
        System.out.printf("  Test cases: %d%n", config.tests().size());
        System.out.println("=".repeat(60));

        List<TriggerEvalResult> results;
        try (TriggerJudge judge = new TriggerJudge(agent, model, config.skill(), skillDescription)) {
            judge.open();
            results = judge.evaluateAll(config.tests());
        }

        Path outputDir = Path.of("target", "eval-results").toAbsolutePath();
        TriggerEvalReport.printConsole(config.skill(), agent, model, results);
        TriggerEvalReport.writeJson(config.skill(), agent, model, results, outputDir);

        long correct = results.stream().filter(TriggerEvalResult::correct).count();
        long total = results.size();
        double accuracy = total == 0 ? 0.0 : (double) correct / total;

        double minAccuracy = Double.parseDouble(
                System.getProperty("eval.min.accuracy", "0.80"));

        assertTrue(accuracy >= minAccuracy,
                "Trigger accuracy %.0f%% (%d/%d) is below threshold %.0f%%"
                        .formatted(accuracy * 100, correct, total, minAccuracy * 100));
    }

    private TriggerEvalConfig loadTestConfig() throws Exception {
        String testFile = System.getProperty("eval.tests", "evals/trigger-tests.json");

        InputStream is = getClass().getClassLoader().getResourceAsStream(testFile);
        if (is != null) {
            return JSON.readValue(is, TriggerEvalConfig.class);
        }

        Path filePath = Path.of(testFile);
        if (Files.exists(filePath)) {
            return JSON.readValue(filePath.toFile(), TriggerEvalConfig.class);
        }

        throw new IllegalStateException(
                "Test config not found: " + testFile + " (tried classpath and filesystem)");
    }

    private String loadSkillDescription(String skillName) throws Exception {
        Path skillDir = skillsDir().resolve(skillName);
        Path skillFile = skillDir.resolve("SKILL.md");

        if (!Files.exists(skillFile)) {
            throw new IllegalStateException("SKILL.md not found at: " + skillFile);
        }

        String content = Files.readString(skillFile);
        return extractYamlDescription(content);
    }

    static String extractYamlDescription(String skillMdContent) {
        if (!skillMdContent.startsWith("---")) {
            throw new IllegalStateException("SKILL.md does not start with YAML frontmatter (---)");
        }

        int secondDash = skillMdContent.indexOf("---", 3);
        if (secondDash < 0) {
            throw new IllegalStateException("SKILL.md has unclosed YAML frontmatter");
        }

        String frontmatter = skillMdContent.substring(3, secondDash).trim();

        JsonNode root = YAML.readTree(frontmatter);
        JsonNode desc = root.path("description");
        if (desc.isMissingNode() || desc.asText("").isEmpty()) {
            throw new IllegalStateException("No 'description' field found in SKILL.md frontmatter");
        }
        return desc.asText();
    }
}
