package com.island.nature.entities.components;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComponentThreadSafetyTest {

    @Test
    void shouldEnsureHealthComponentVisibility() throws InterruptedException {
        HealthComponent hc = new HealthComponent(100, 100, true);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                startLatch.await();
                hc.setCurrentEnergy(50);
                hc.setAlive(false);
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        writer.start();
        startLatch.countDown();
        
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
        
        // Volatile ensures visibility
        assertEquals(50, hc.getCurrentEnergy());
        assertFalse(hc.isAlive());
    }

    @Test
    void shouldEnsureAgeComponentVisibility() throws InterruptedException {
        AgeComponent ac = new AgeComponent(0, 100);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);

        Thread writer = new Thread(() -> {
            try {
                startLatch.await();
                ac.setAge(10);
                doneLatch.countDown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        writer.start();
        startLatch.countDown();
        
        assertTrue(doneLatch.await(1, TimeUnit.SECONDS));
        
        assertEquals(10, ac.getAge());
    }
}