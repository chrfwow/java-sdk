package dev.openfeature.sdk.testutils;

import com.vmlens.api.AllInterleavings;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;

class VmLensTest {
    int j = 0;

    @Test
    void concurrentEvalAndHooks() throws InterruptedException {
        var c = new AtomicInteger();

        try (AllInterleavings allInterleavings = new AllInterleavings("Concurrent evaluations and hook additions")) {
            while (allInterleavings.hasNext()) {
                c.incrementAndGet();
                j = 0;
                Thread first = new Thread() {
                    @Override
                    public void run() {
                        j++;
                    }
                };
                first.start();
                j++;
                first.join();
                if (j != 2) {
                    throw new RuntimeException("j=" + j);
                }
            }
        }

        System.out.println("c = " + c);
        System.out.println("j = " + j);
    }
}
