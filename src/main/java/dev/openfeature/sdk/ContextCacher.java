package dev.openfeature.sdk;

import java.util.concurrent.atomic.AtomicReference;

class ContextCacher {
    private final AtomicReference<ContextKey> key = new AtomicReference<>(
            new ContextKey(new ImmutableContext(), new ImmutableContext(), new ImmutableContext()));
    private final Client client;
    private final OpenFeatureAPI openfeatureApi;

    ContextCacher(Client client, OpenFeatureAPI openfeatureApi) {
        this.client = client;
        this.openfeatureApi = openfeatureApi;
    }

    // you still need to merge the invocation context onto the result
    EvaluationContext getMergedEvaluationContext() {
        var keyRef = key;
        var currentKey = keyRef.get();
        EvaluationContext apiContext = openfeatureApi.getEvaluationContext();
        EvaluationContext transactionContext = openfeatureApi.getTransactionContext();
        EvaluationContext clientContext = client.getEvaluationContext();

        if (currentKey.equals(apiContext, transactionContext, clientContext)) {
            return currentKey.getMergedContext();
        }

        ContextKey newKey = new ContextKey(apiContext, transactionContext, clientContext);

        while (!key.compareAndSet(currentKey, newKey)) {
            currentKey = keyRef.get();
            apiContext = openfeatureApi.getEvaluationContext();
            transactionContext = openfeatureApi.getTransactionContext();
            clientContext = client.getEvaluationContext();
            newKey = new ContextKey(apiContext, transactionContext, clientContext);
        }

        return key.get().getMergedContext();
    }
}
