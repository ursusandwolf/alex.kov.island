package com.island.engine;

import com.island.engine.internal.AgeSoAStore;
import com.island.engine.internal.HealthSoAStore;
import com.island.engine.internal.MovementSoAStore;
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
        
        // Out of bounds read
        assertEquals(0L, store.getCurrentEnergy(100));
        assertEquals(0L, store.getMaxEnergy(100));
        assertFalse(store.isAlive(100));
        
        // Out of bounds write (no-op or expansion)
        store.setCurrentEnergy(100, 50L); // No-op
        assertEquals(0L, store.getCurrentEnergy(100));
        
        store.setAlive(100, true); // No-op
        assertFalse(store.isAlive(100));
        
        assertEquals(0L, store.addEnergy(100, 10L)); // No-op
    }

    @Test
    @DisplayName("HealthSoAStore: Capacity expansion")
    void healthStore_expansion() {
        HealthSoAStore store = new HealthSoAStore(2);
        store.set(5, 10L, 20L, true); // Trigger expansion
        
        assertEquals(10L, store.getCurrentEnergy(5));
        assertTrue(store.isAlive(5));
        
        store.set(1, 5L, 10L, true); // Update existing
        assertEquals(5L, store.getCurrentEnergy(1));
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
        
        store.setMaxLifespan(1, 12);
        assertEquals(12, store.getMaxLifespan(1));

        // Out of bounds
        assertEquals(0, store.getAge(100));
        assertEquals(0, store.getMaxLifespan(100));
        store.setAge(100, 10); // No-op
        assertEquals(0, store.getAge(100));
    }

    @Test
    @DisplayName("AgeSoAStore: Expansion")
    void ageStore_expansion() {
        AgeSoAStore store = new AgeSoAStore(2);
        store.set(5, 1, 2);
        assertEquals(1, store.getAge(5));
    }

    @Test
    @DisplayName("MovementSoAStore: Basic set and get")
    void movementStore_basic_ops() {
        MovementSoAStore store = new MovementSoAStore(10);
        store.set(1, 3);
        assertEquals(3, store.getSpeed(1));
        
        store.setSpeed(1, 4);
        assertEquals(4, store.getSpeed(1));
        
        // Out of bounds
        assertEquals(0, store.getSpeed(100));
    }

    @Test
    @DisplayName("MovementSoAStore: Expansion")
    void movementStore_expansion() {
        MovementSoAStore store = new MovementSoAStore(2);
        store.set(5, 2);
        assertEquals(2, store.getSpeed(5));
    }

    @Test
    @DisplayName("Thread Safety: Concurrent updates and resizing")
    void store_thread_safety() throws InterruptedException {
        final int entities = 100;
        final int iterations = 1000;
        HealthSoAStore store = new HealthSoAStore(10); // Start small to trigger resizing
        ExecutorService executor = Executors.newFixedThreadPool(4);
        
        for (int i = 0; i < 4; i++) {
            executor.submit(() -> {
                for (int j = 0; j < iterations; j++) {
                    for (int id = 0; id < entities; id++) {
                        // Mix of reads, writes and resizing
                        store.set(id, 100L, 200L, true);
                        store.addEnergy(id, 1L);
                        store.getCurrentEnergy(id);
                    }
                }
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        
        for (int id = 0; id < entities; id++) {
            // Since we set 100 and then add 1, final value depends on race if not atomic.
            // But set() and addEnergy() are both thread-safe.
            // However, the sequence set -> add is NOT atomic together.
            // But we can check that it didn't crash and value is reasonable.
            long energy = store.getCurrentEnergy(id);
            assertTrue(energy >= 100L);
        }
    }
}
