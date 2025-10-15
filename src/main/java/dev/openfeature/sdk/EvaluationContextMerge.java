package dev.openfeature.sdk;

import java.util.HashMap;
import java.util.Map;

class EvaluationContextMerge {
    private EvaluationContextMerge() {}

    static EvaluationContext mergeContextMaps(EvaluationContext... contexts) {
        // avoid any unnecessary context instantiations and stream usage here; this is
        // called with every evaluation.
        Map merged = new HashMap<>();
        for (EvaluationContext evaluationContext : contexts) {
            if (evaluationContext != null && !evaluationContext.isEmpty()) {
                EvaluationContext.mergeMaps(ImmutableStructure::new, merged, evaluationContext.asUnmodifiableMap());
            }
        }
        return new ImmutableContext(merged);
    }
}
