package dev.openfeature.sdk;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ContextCacherTest {
    @Test
    void testGetMergedEvaluationContextDoesNotReturnNullWhenAllContextsAreNull() {
        // Mocks return null for all contexts
        Client client = Mockito.mock(Client.class);
        Mockito.when(client.getEvaluationContext()).thenReturn(null);
        OpenFeatureAPI api = Mockito.mock(OpenFeatureAPI.class);
        Mockito.when(api.getEvaluationContext()).thenReturn(null);
        Mockito.when(api.getTransactionContext()).thenReturn(null);

        var cacher = new ContextCacher(client, api);
        var merged = cacher.getMergedEvaluationContext();
        assertNotNull(merged);
    }
}
