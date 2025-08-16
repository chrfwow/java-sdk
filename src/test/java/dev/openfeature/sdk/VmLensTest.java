package dev.openfeature.sdk;

import com.vmlens.api.AllInterleavings;
import com.vmlens.api.Runner;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Javadoc.
 */
public class VmLensTest {
    public static void main(String[] args) throws InterruptedException {
        new VmLensTest().asomeMethod();
    }

    @Test
    public void asomeMethod() throws InterruptedException {
        var c = new AtomicInteger();
        final OpenFeatureAPI api = new OpenFeatureAPI();

        var flags = new HashMap<String, Flag<?>>();
        flags.put("a", Flag.builder().variant("a", "def").defaultVariant("a").build());
        flags.put("b", Flag.builder().variant("a", "as").defaultVariant("a").build());
        flags.put("c", Flag.builder().variant("a", "dfs").defaultVariant("a").build());
        flags.put("d", Flag.builder().variant("a", "asddd").defaultVariant("a").build());
        api.setProviderAndWait(new InMemoryProvider(flags));

        try (AllInterleavings allInterleavings = new AllInterleavings("Concurrent evaluations and hook additions")) {
            while (allInterleavings.hasNext()) {
                var client = api.getClient();
                c.incrementAndGet();
                Runner.runParallel(
                        () -> client.getStringValue("a", "a"),
                        () -> client.addHooks(new Hook() {})
                );
            }
        }
        api.shutdown();
        System.out.println("c = " + c);
    }
}
