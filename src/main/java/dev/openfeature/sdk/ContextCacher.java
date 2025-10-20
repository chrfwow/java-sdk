package dev.openfeature.sdk;

import java.util.concurrent.atomic.AtomicReference;

class ContextCacher {
    private final AtomicReference<ApiTransactionClientContextKey> fullKey =
            new AtomicReference<>(new ApiTransactionClientContextKey(
                    new ApiClientContextKey(new ImmutableContext(), new ImmutableContext()),
                    new ImmutableContext(),
                    null));

    private final Client client;
    private final OpenFeatureAPI openfeatureApi;

    ContextCacher(Client client, OpenFeatureAPI openfeatureApi) {
        this.client = client;
        this.openfeatureApi = openfeatureApi;
    }

    // you still need to merge the invocation context onto the result
    EvaluationContext getMergedEvaluationContext() {
        var currentKey = fullKey.get();
        EvaluationContext apiContext = openfeatureApi.getEvaluationContext();
        EvaluationContext clientContext = client.getEvaluationContext();
        EvaluationContext transactionContext = openfeatureApi.getTransactionContext();

        if (currentKey.equals(apiContext, transactionContext, clientContext)) {
            return currentKey.getMergedContext();
        }

        var newPartKey = new ApiClientContextKey(apiContext, clientContext);
        var newKey = new ApiTransactionClientContextKey(newPartKey, transactionContext, currentKey.apiContextKey);

        while (!fullKey.compareAndSet(currentKey, newKey)) {
            currentKey = fullKey.get();
            apiContext = openfeatureApi.getEvaluationContext();
            clientContext = client.getEvaluationContext();
            transactionContext = openfeatureApi.getTransactionContext();
            newPartKey = new ApiClientContextKey(apiContext, clientContext);
            newKey = new ApiTransactionClientContextKey(newPartKey, transactionContext, currentKey.apiContextKey);
        }

        return fullKey.get().getMergedContext();
    }
}
