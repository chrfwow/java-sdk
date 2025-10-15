package dev.openfeature.sdk.testutils;

import dev.openfeature.sdk.EvaluationContext;
import dev.openfeature.sdk.TransactionContextPropagator;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryTransactionContextPropagator implements TransactionContextPropagator {
    private final AtomicReference<EvaluationContext> evaluationContext = new AtomicReference<>();

    @Override
    public EvaluationContext getTransactionContext() {
        return evaluationContext.get();
    }

    @Override
    public void setTransactionContext(EvaluationContext evaluationContext) {
        this.evaluationContext.set(evaluationContext);
    }
}
