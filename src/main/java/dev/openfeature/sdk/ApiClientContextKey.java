package dev.openfeature.sdk;

class ApiClientContextKey {
    final EvaluationContext apiContext;
    final EvaluationContext clientContext;
    volatile EvaluationContext mergedContext = null;

    ApiClientContextKey(EvaluationContext apiContext, EvaluationContext clientContext) {
        this.apiContext = apiContext;
        this.clientContext = clientContext;
    }

    EvaluationContext getMergedContext() {
        final var currentMergedContext = mergedContext;
        if (currentMergedContext != null) {
            // short circuit if it is already set
            return currentMergedContext;
        }
        var newMergedContext = EvaluationContextMerge.mergeContextMaps(apiContext, clientContext);
        // if not set, compute it ourselves and set it.
        // We don't care if another thread sets it first, and we override it, it should have the same value
        mergedContext = newMergedContext;
        // even if another thread won, this is still the correct value as all contexts are final
        return newMergedContext;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ApiClientContextKey)) {
            return false;
        }
        final ApiClientContextKey ck = (ApiClientContextKey) other;
        return apiContext == ck.apiContext && clientContext == ck.clientContext;
    }

    public boolean equals(EvaluationContext apiContext, EvaluationContext clientContext) {
        return this.apiContext == apiContext && clientContext == this.clientContext;
    }

    @Override
    public int hashCode() {
        return apiContext.hashCode() + clientContext.hashCode();
    }
}
