package com.island.engine;

import com.island.engine.internal.AgeSoAStore;
import com.island.engine.internal.HealthSoAStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class SoAStoreTest {

    @Test
    @DisplayName("HealthSoAStore: Basic set and get")
    void healthStore_basic_ops() {
        HealthSoAStore store = new HealthSoAStore(10);
        store.set(1, 100L, 200L, true);
        
        assertEquals(100L, store.getCurrentEnergy(1));
        assertEquals(200L, store.getMaxEnergy(1));
        assertTrue(store.isAlive(1));
        
        store.setCurrentEnergy(1, 50L);
        assertEquals(50L, store.getCurrentEnergy(1));
        
        store.setAlive(1, false);
        assertFalse(store.isAlive(1));
    }

    @Test
    @DisplayName("HealthSoAStore: Capacity expansion")
    void healthStore_expansion() {
        HealthSoAStore store = new HealthSoAStore(2);
        store.set(5, 10L, 20L, true); // Trigger expansion
        
        assertEquals(10L, store.getCurrentEnergy(5));
        assertTrue(store.isAlive(5));
    }

    @Test
    @DisplayName("AgeSoAStore: Basic set and get")
    void ageStore_basic_ops() {
        AgeSoAStore store = new AgeSoAStore(10);
        store.set(1, 5, 10);
        
        assertEquals(5, store.getAge(1));
        assertEquals(10, store.getMaxLifespan(1));
        
        store.setAge(1, 6);
        assertEquals(6, store.getAge(1));
    }

    @Test
    @DisplayName("Thread Safety: Concurrent updates to SoA Store")
    void store_thread_safety() throws InterruptedException {
        final int entities = 100;
        final int iterations = 1000;
        HealthSoAStore store = new HealthSoAStore(entities);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    for (int id = 0; id < entities; id++) {
                        store.addEnergy(id, 1L);
                    }
                }
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        
        for (int id = 0; id < entities; id++) {
            assertEquals(4000L, store.getCurrentEnergy(id), "Energy mismatch for entity " + id);
        }
    }
}
