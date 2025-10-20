package dev.openfeature.sdk;

class ApiTransactionClientContextKey {
    final ApiClientContextKey apiContextKey;
    final EvaluationContext transactionContext;
    final ApiClientContextKey oldPartKey;
    volatile EvaluationContext mergedContext = null;

    ApiTransactionClientContextKey(
            ApiClientContextKey apiContextKey, EvaluationContext transactionContext, ApiClientContextKey oldPartKey) {
        this.apiContextKey = apiContextKey;
        this.transactionContext = transactionContext;
        this.oldPartKey = oldPartKey;
    }

    EvaluationContext getMergedContext() {
        final var currentMergedContext = mergedContext;
        if (currentMergedContext != null) {
            // short circuit if it is already set
            return currentMergedContext;
        }

        EvaluationContext newMergedContext = null;
        if (oldPartKey == null) {
            // no information about the previous contexts
            newMergedContext = EvaluationContextMerge.mergeContextMaps(
                    apiContextKey.apiContext, transactionContext, apiContextKey.clientContext);
        } else {
            if (oldPartKey.equals(apiContextKey) && oldPartKey.mergedContext != null) {
                // we can reuse the old context, if transaction context does not conflict with it
                var transactionMap = transactionContext.asUnmodifiableMap();
                var oldMergedContextMap = oldPartKey.mergedContext.asUnmodifiableMap();
                boolean conflict = false;
                for (var entry : transactionMap.entrySet()) {
                    if (oldMergedContextMap.containsKey(entry.getKey())) {
                        // we found a conflict and cannot reuse the old context
                        conflict = true;
                        newMergedContext = EvaluationContextMerge.mergeContextMaps(
                                apiContextKey.apiContext, transactionContext, apiContextKey.clientContext);
                        break;
                    }
                }
                if (!conflict) {
                    newMergedContext =
                            EvaluationContextMerge.mergeContextMaps(oldPartKey.mergedContext, transactionContext);
                }
            } else {
                // api or client context changed, we cannot reuse old context data, recompute
                newMergedContext = EvaluationContextMerge.mergeContextMaps(
                        apiContextKey.apiContext, transactionContext, apiContextKey.clientContext);
            }
        }

        // if not set, compute it ourselves and set it.
        // We don't care if another thread sets it first, and we override it, it should have the same value
        mergedContext = newMergedContext;
        // even if another thread won, this is still the correct value as all contexts are final
        return newMergedContext;
    }

    @Override
    public int hashCode() {
        return apiContextKey.hashCode() + transactionContext.hashCode() + oldPartKey.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ApiTransactionClientContextKey) {
            ApiTransactionClientContextKey other = (ApiTransactionClientContextKey) obj;
            return apiContextKey == other.apiContextKey
                    && transactionContext == other.transactionContext
                    && oldPartKey == other.oldPartKey;
        }
        return false;
    }

    public boolean equals(
            EvaluationContext apiContext, EvaluationContext transactionContext, EvaluationContext clientContext) {
        return this.apiContextKey.equals(apiContext, clientContext) && transactionContext == this.transactionContext;
    }
}
