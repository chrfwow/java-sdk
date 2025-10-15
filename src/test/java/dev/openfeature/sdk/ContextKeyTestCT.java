package dev.openfeature.sdk;

import static org.junit.jupiter.api.Assertions.*;

import com.vmlens.api.AllInterleavings;
import com.vmlens.api.Runner;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ContextKeyTestCT {
    @Test
    void testGetMergedContextReturnsCachedValueOnSecondInvocation() {
        var apiCtx = new ImmutableContext(Map.of("key1", new Value("api1")));
        var txnCtx = new ImmutableContext(Map.of("key2", new Value("txn2")));
        var clientCtx = new ImmutableContext(Map.of("key3", new Value("client3")));
        ContextKey key = new ContextKey(apiCtx, txnCtx, clientCtx);

        try (AllInterleavings allInterleavings = new AllInterleavings("ContextKey caches merged context")) {
            var first = new AtomicReference<EvaluationContext>();
            var second = new AtomicReference<EvaluationContext>();
            while (allInterleavings.hasNext()) {
                Runner.runParallel(() -> first.set(key.getMergedContext()), () -> second.set(key.getMergedContext()));
                assertSame(first.get(), second.get());
            }
        }
    }
}
