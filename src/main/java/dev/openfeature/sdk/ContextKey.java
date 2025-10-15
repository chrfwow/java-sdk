package dev.openfeature.sdk;

class ContextKey {
    private final EvaluationContext apiContext;
    private final EvaluationContext transactionContext;
    private final EvaluationContext clientContext;
    private volatile EvaluationContext mergedContext = null;

    ContextKey(EvaluationContext apiContext, EvaluationContext transactionContext, EvaluationContext clientContext) {
        this.apiContext = apiContext;
        this.transactionContext = transactionContext;
        this.clientContext = clientContext;
    }

    EvaluationContext getMergedContext() {
        final var currentMergedContext = mergedContext;
        if (currentMergedContext != null) {
            // short circuit if it is already set
            return currentMergedContext;
        }
        var newMergedContext = EvaluationContextMerge.mergeContextMaps(apiContext, transactionContext, clientContext);
        // if not set, compute it ourselves and set it.
        // We don't care if another thread sets it first, and we override it, it should have the same value
        mergedContext = newMergedContext;
        // even if another thread won, this is still the correct value as all contexts are final
        return newMergedContext;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ContextKey)) {
            return false;
        }
        final ContextKey ck = (ContextKey) other;
        return apiContext == ck.apiContext
                && transactionContext == ck.transactionContext
                && clientContext == ck.clientContext;
    }

    public boolean equals(
            EvaluationContext apiContext, EvaluationContext transactionContext, EvaluationContext clientContext) {
        return apiContext == this.apiContext
                && transactionContext == this.transactionContext
                && clientContext == this.clientContext;
    }

    @Override
    public int hashCode() {
        return (apiContext == null ? 0 : apiContext.hashCode())
                + (transactionContext == null ? 0 : transactionContext.hashCode())
                + (clientContext == null ? 0 : clientContext.hashCode());
    }
}
