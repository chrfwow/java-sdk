package dev.openfeature.sdk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.vmlens.api.AllInterleavings;
import dev.openfeature.sdk.exceptions.FatalError;
import dev.openfeature.sdk.fixtures.HookFixtures;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import dev.openfeature.sdk.testutils.TestEventsProvider;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.simplify4u.slf4jmock.LoggerMock;
import org.slf4j.Logger;

class OpenFeatureClientTest implements HookFixtures {

    private Logger logger;

    @BeforeEach
    void set_logger() {
        logger = Mockito.mock(Logger.class);
        LoggerMock.setMock(OpenFeatureClient.class, logger);
    }

    @AfterEach
    void reset_logs() {
        LoggerMock.setMock(OpenFeatureClient.class, logger);
    }

    @Test
    @DisplayName("should not throw exception if hook has different type argument than hookContext")
    void shouldNotThrowExceptionIfHookHasDifferentTypeArgumentThanHookContext() {
        OpenFeatureAPI api = new OpenFeatureAPI();
        api.setProviderAndWait(
                "shouldNotThrowExceptionIfHookHasDifferentTypeArgumentThanHookContext", new DoSomethingProvider());
        Client client = api.getClient("shouldNotThrowExceptionIfHookHasDifferentTypeArgumentThanHookContext");
        client.addHooks(mockBooleanHook(), mockStringHook());
        FlagEvaluationDetails<Boolean> actual = client.getBooleanDetails("feature key", Boolean.FALSE);

        assertThat(actual.getValue()).isTrue();
        // I dislike this, but given the mocking tools available, there's no way that I know of to say "no errors were
        // logged"
        Mockito.verify(logger, never()).error(any());
        Mockito.verify(logger, never()).error(anyString(), any(Throwable.class));
        Mockito.verify(logger, never()).error(anyString(), any(Object.class));
        Mockito.verify(logger, never()).error(anyString(), any(), any());
        Mockito.verify(logger, never()).error(anyString(), any(), any());
    }

    @Test
    @DisplayName("addHooks should allow chaining by returning the same client instance")
    void addHooksShouldAllowChaining() {
        OpenFeatureAPI api = mock(OpenFeatureAPI.class);
        OpenFeatureClient client = new OpenFeatureClient(api, "name", "version");
        Hook<?> hook1 = Mockito.mock(Hook.class);
        Hook<?> hook2 = Mockito.mock(Hook.class);

        OpenFeatureClient result = client.addHooks(hook1, hook2);
        assertEquals(client, result);
    }

    @Test
    @DisplayName("setEvaluationContext should allow chaining by returning the same client instance")
    void setEvaluationContextShouldAllowChaining() {
        OpenFeatureAPI api = mock(OpenFeatureAPI.class);
        OpenFeatureClient client = new OpenFeatureClient(api, "name", "version");
        EvaluationContext ctx = new ImmutableContext("targeting key", new HashMap<>());

        OpenFeatureClient result = client.setEvaluationContext(ctx);
        assertEquals(client, result);
    }

    @Test
    @DisplayName("Should not call evaluation methods when the provider has state FATAL")
    void shouldNotCallEvaluationMethodsWhenProviderIsInFatalErrorState() {
        FeatureProvider provider = new TestEventsProvider(100, true, "fake fatal", true);
        OpenFeatureAPI api = new OpenFeatureAPI();
        Client client = api.getClient("shouldNotCallEvaluationMethodsWhenProviderIsInFatalErrorState");

        assertThrows(
                FatalError.class,
                () -> api.setProviderAndWait(
                        "shouldNotCallEvaluationMethodsWhenProviderIsInFatalErrorState", provider));
        FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("key", true);
        assertThat(details.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_FATAL);
    }

    @Test
    @DisplayName("Should not call evaluation methods when the provider has state NOT_READY")
    void shouldNotCallEvaluationMethodsWhenProviderIsInNotReadyState() {
        FeatureProvider provider = new TestEventsProvider(5000);
        OpenFeatureAPI api = new OpenFeatureAPI();
        api.setProvider("shouldNotCallEvaluationMethodsWhenProviderIsInNotReadyState", provider);
        Client client = api.getClient("shouldNotCallEvaluationMethodsWhenProviderIsInNotReadyState");
        FlagEvaluationDetails<Boolean> details = client.getBooleanDetails("key", true);

        assertThat(details.getErrorCode()).isEqualTo(ErrorCode.PROVIDER_NOT_READY);
    }

    private int j = 0;

    @Test
    public void testUpdate() throws InterruptedException {
        OpenFeatureAPI api = new OpenFeatureAPI();

        var flags = new HashMap<String, Flag<?>>();
        flags.put("a", Flag.builder().variant("a", "def").defaultVariant("a").build());
        flags.put("b", Flag.builder().variant("a", "as").defaultVariant("a").build());
        flags.put("c", Flag.builder().variant("a", "dfs").defaultVariant("a").build());
        flags.put("d", Flag.builder().variant("a", "asddd").defaultVariant("a").build());
        api.setProviderAndWait(new InMemoryProvider(flags));

        try (AllInterleavings allInterleavings = new AllInterleavings("Concurrent evaluations and hook additions")) {
            while (allInterleavings.hasNext()) {
                var latch = new CountDownLatch(1);
                var client = api.getClient();
                var readyLatch = new CountDownLatch(2);
                var concurrentModException = new AtomicReference<ConcurrentModificationException>();
                Thread eval = new Thread(() -> {
                    readyLatch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        client.getStringValue("a", "a");
                        client.getStringValue("b", "a");
                        client.getStringValue("c", "a");
                        client.getStringValue("d", "a");
                    } catch (ConcurrentModificationException e) {
                        concurrentModException.set(e);
                    }
                });
                Thread hookAdder = new Thread(() -> {
                    readyLatch.countDown();
                    try {
                        latch.await();
                    } catch (InterruptedException ignored) {
                    }
                    try {
                        client.addHooks(new Hook() {});
                    } catch (ConcurrentModificationException e) {
                        concurrentModException.set(e);
                    }
                });
                eval.start();
                hookAdder.start();
                readyLatch.await();
                latch.countDown();
                eval.join();
                hookAdder.join();
                assertNotNull(concurrentModException.get());
            }
        }
    }
}
