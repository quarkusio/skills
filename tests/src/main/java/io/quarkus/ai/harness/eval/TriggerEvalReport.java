package io.quarkus.ai.harness.eval;

import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TriggerEvalReport {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter
            .ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    private TriggerEvalReport() {}

    public static void printConsole(String skill, String agent, String model, List<TriggerEvalResult> results) {
        long positiveTotal = results.stream().filter(TriggerEvalResult::expectedTrigger).count();
        long positiveCorrect = results.stream().filter(r -> r.expectedTrigger() && r.correct()).count();
        long negativeTotal = results.stream().filter(r -> !r.expectedTrigger()).count();
        long negativeCorrect = results.stream().filter(r -> !r.expectedTrigger() && r.correct()).count();
        long total = results.size();
        long correct = results.stream().filter(TriggerEvalResult::correct).count();

        System.out.println();
        System.out.println("=".repeat(60));
        System.out.printf("  Trigger Eval: %s%n", skill);
        System.out.printf("  Judge: %s / %s%n", agent, model.isEmpty() ? "(agent default)" : model);
        System.out.println("=".repeat(60));
        System.out.printf("  Positive (should trigger):     %d/%d (%.0f%%)%n",
                positiveCorrect, positiveTotal, pct(positiveCorrect, positiveTotal));
        System.out.printf("  Negative (should NOT trigger): %d/%d (%.0f%%)%n",
                negativeCorrect, negativeTotal, pct(negativeCorrect, negativeTotal));
        System.out.printf("  Overall accuracy:              %d/%d (%.0f%%)%n",
                correct, total, pct(correct, total));

        List<TriggerEvalResult> failures = results.stream().filter(r -> !r.correct()).toList();
        if (!failures.isEmpty()) {
            System.out.println();
            System.out.println("  Failures:");
            for (TriggerEvalResult f : failures) {
                System.out.printf("    %s: expected trigger=%b, got trigger=%b (confidence=%.2f)%n",
                        f.id(), f.expectedTrigger(), f.actualTrigger(), f.confidence());
                System.out.printf("      Reasoning: %s%n", f.reasoning());
            }
        }
        System.out.println("=".repeat(60));
    }

    public static void writeJson(String skill, String agent, String model,
                                 List<TriggerEvalResult> results, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);

        long correct = results.stream().filter(TriggerEvalResult::correct).count();
        long positiveTotal = results.stream().filter(TriggerEvalResult::expectedTrigger).count();
        long positiveCorrect = results.stream().filter(r -> r.expectedTrigger() && r.correct()).count();
        long negativeTotal = results.stream().filter(r -> !r.expectedTrigger()).count();
        long negativeCorrect = results.stream().filter(r -> !r.expectedTrigger() && r.correct()).count();

        Instant now = Instant.now();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("timestamp", now.toString());
        report.put("skill", skill);
        report.put("judge_agent", agent);
        report.put("judge_model", model.isEmpty() ? "(agent default)" : model);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", results.size());
        summary.put("correct", correct);
        summary.put("accuracy", results.isEmpty() ? 0.0 : (double) correct / results.size());
        summary.put("positive_accuracy", positiveTotal == 0 ? 0.0 : (double) positiveCorrect / positiveTotal);
        summary.put("negative_accuracy", negativeTotal == 0 ? 0.0 : (double) negativeCorrect / negativeTotal);
        report.put("summary", summary);

        List<Map<String, Object>> resultList = results.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.id());
            m.put("prompt", r.prompt());
            m.put("expected_trigger", r.expectedTrigger());
            m.put("actual_trigger", r.actualTrigger());
            m.put("correct", r.correct());
            m.put("confidence", r.confidence());
            m.put("reasoning", r.reasoning());
            if (r.category() != null) m.put("category", r.category());
            return m;
        }).toList();
        report.put("results", resultList);

        Path file = outputDir.resolve("trigger-eval-" + FILE_TIMESTAMP.format(now) + ".json");

        var mapper = JsonMapper.builder()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
        mapper.writeValue(file.toFile(), report);

        System.out.println("  Report written to: " + file);
    }

    private static double pct(long correct, long total) {
        return total == 0 ? 0.0 : 100.0 * correct / total;
    }
}
