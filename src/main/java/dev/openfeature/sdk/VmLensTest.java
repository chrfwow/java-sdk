package dev.openfeature.sdk;

import com.vmlens.api.AllInterleavings;
import dev.openfeature.sdk.providers.memory.Flag;
import dev.openfeature.sdk.providers.memory.InMemoryProvider;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Javadoc.
 */
public class VmLensTest {
    int jaVar = 0;


    public static void main(String[] args) throws InterruptedException {
        new VmLensTest().asomeMethod();
    }

    private void asomeMethod() throws InterruptedException {
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
                c.incrementAndGet();
                var client = api.getClient();
                var firstReady = new Awaitable();
                Thread first = new Thread("test thread") {
                    @Override
                    public void run() {
                        firstReady.wakeup();
                        client.getStringValue("a", "a");
                        //client.getStringValue("a", "a");
                    }
                };

                first.start();
                firstReady.await();

                client.addHooks(new Hook() {});
                //client.addHooks(new Hook() {});

                first.join();
            }
        }

        api.shutdown();

        System.out.println("c = " + c);
        System.out.println("jaVar = " + jaVar);

        //Thread.sleep(5000);
        //System.exit(0);
    }
}
