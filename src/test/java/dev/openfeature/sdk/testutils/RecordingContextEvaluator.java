package dev.openfeature.sdk.testutils;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.providers.memory.ContextEvaluator;
import dev.openfeature.sdk.providers.memory.Flag;

public class RecordingContextEvaluator implements ContextEvaluator {
    public EvaluationContext evaluationContext;

    @Override
    public Object evaluate(Flag flag, EvaluationContext evaluationContext) {
        this.evaluationContext = evaluationContext;
        return flag.getVariants().get(flag.getDefaultVariant());
    }
}
