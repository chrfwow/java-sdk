package dev.openfeature.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ContextKeyTest {
    @Test
    void testEqualsDoesNotThrowWhenAllContextsAreNull() {
        ContextKey key1 = new ContextKey(null, null, null);
        ContextKey key2 = new ContextKey(null, null, null);
        assertDoesNotThrow(() -> key1.equals(key2));
        assertDoesNotThrow(() -> key1.equals(null));
    }

    @Test
    void testEqualsMethodDoesNotThrowNPE() {
        ContextKey key = new ContextKey(null, null, null);
        assertDoesNotThrow(() -> key.equals(null, null, null));
    }

    @Test
    void testHashCodeDoesNotThrowWhenAllContextsAreNull() {
        ContextKey key = new ContextKey(null, null, null);
        assertDoesNotThrow(key::hashCode);
    }

    @Test
    void testContextsAreMergedInCorrectOrder() {
        // API context
        var apiCtx = new ImmutableContext(Map.of(
                "key1", new Value("api1"),
                "key2", new Value("api2")));
        // Transaction context overrides key2
        var txnCtx = new ImmutableContext(Map.of(
                "key2", new Value("txn2"),
                "key3", new Value("txn3")));
        // Client context overrides key1 and adds key4
        var clientCtx = new ImmutableContext(Map.of(
                "key1", new Value("client1"),
                "key4", new Value("client4")));

        ContextKey key = new ContextKey(apiCtx, txnCtx, clientCtx);
        var merged = key.getMergedContext();

        // key1 should be from client, key2 from transaction, key3 from transaction, key4 from client
        assertEquals("client1", merged.getValue("key1").asString());
        assertEquals("txn2", merged.getValue("key2").asString());
        assertEquals("txn3", merged.getValue("key3").asString());
        assertEquals("client4", merged.getValue("key4").asString());
    }

    @Test
    void testGetMergedContextReturnsCachedValueOnSecondInvocation() {
        var apiCtx = new ImmutableContext(Map.of("key1", new Value("api1")));
        var txnCtx = new ImmutableContext(Map.of("key2", new Value("txn2")));
        var clientCtx = new ImmutableContext(Map.of("key3", new Value("client3")));
        ContextKey key = new ContextKey(apiCtx, txnCtx, clientCtx);
        EvaluationContext first = key.getMergedContext();
        EvaluationContext second = key.getMergedContext();
        assertSame(first, second);
    }
}
