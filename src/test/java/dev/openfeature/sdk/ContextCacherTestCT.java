package dev.openfeature.sdk;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vmlens.api.AllInterleavings;
import com.vmlens.api.Runner;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import dev.openfeature.sdk.testutils.InMemoryTransactionContextPropagator;
import dev.openfeature.sdk.testutils.RecordingContextEvaluator;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextCacherTestCT {
    @Test
    void testGetMergedEvaluationContextDoesNotReturnNullWhenAllContextsAreNull() {
        var recorder = new RecordingContextEvaluator();
        var api = new OpenFeatureAPI();
        api.setTransactionContextPropagator(new InMemoryTransactionContextPropagator());
        api.setProviderAndWait(new InMemoryProvider(Map.of(
                "flag",
                Flag.<Boolean>builder()
                        .contextEvaluator(recorder)
                        .variant("key", Boolean.TRUE)
                        .build())));
        var client = api.getClient();

        // Create two distinct evaluation contexts for each level
        EvaluationContext clientContext1 = new ImmutableContext(Map.of("client", new Value("client1")));
        EvaluationContext clientContext2 = new ImmutableContext(Map.of("client", new Value("client2")));
        EvaluationContext apiContext1 = new ImmutableContext(Map.of("api", new Value("api1")));
        EvaluationContext apiContext2 = new ImmutableContext(Map.of("api", new Value("api2")));
        EvaluationContext transactionContext1 = new ImmutableContext(Map.of("transaction", new Value("tx1")));
        EvaluationContext transactionContext2 = new ImmutableContext(Map.of("transaction", new Value("tx2")));

        var clientContexts = new EvaluationContext[] {clientContext1, clientContext2};
        var apiContexts = new EvaluationContext[] {apiContext1, apiContext2};
        var transactionContexts = new EvaluationContext[] {transactionContext1, transactionContext2};

        var possibleMergedContexts =
                new EvaluationContext[clientContexts.length * apiContexts.length * transactionContexts.length];
        int index = 0;
        for (var cc : clientContexts) {
            for (var ac : apiContexts) {
                for (var tc : transactionContexts) {
                    possibleMergedContexts[index++] = EvaluationContextMerge.mergeContextMaps(ac, tc, cc);
                }
            }
        }

        api.setEvaluationContext(apiContext1);
        api.setTransactionContext(transactionContext1);
        client.setEvaluationContext(clientContext1);

        try (AllInterleavings allInterleavings =
                new AllInterleavings("ContextCacher handles concurrent context updates", true)) {
            while (allInterleavings.hasNext()) {
                Runner.runParallel(
                        () -> api.setEvaluationContext(apiContext2),
                        () -> api.setTransactionContext(transactionContext2),
                        () -> client.setEvaluationContext(clientContext2),
                        () -> client.getBooleanDetails("flag", false, null));
                var recordedContext = recorder.evaluationContext;
                assertTrue(Arrays.stream(possibleMergedContexts).anyMatch(ctx -> recordedContext.equals(ctx)));
            }
        }
    }
}
