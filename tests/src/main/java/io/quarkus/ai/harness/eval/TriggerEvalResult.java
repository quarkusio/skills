package io.quarkus.ai.harness.eval;

public record TriggerEvalResult(
        String id,
        String prompt,
        boolean expectedTrigger,
        boolean actualTrigger,
        double confidence,
        String reasoning,
        String category
) {
    public boolean correct() {
        return expectedTrigger == actualTrigger;
    }
}
