package com.island.service;

import com.island.content.animals.predators.Wolf;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesKey;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementServiceTest {
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @Test
    void testConcurrentMovement() throws InterruptedException {
        Island island = new Island(2, 1);
        Cell cell0 = island.getCell(0, 0);
        
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        cell0.addAnimal(wolf);
        
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        MovementService service = new MovementService(island, executor);
        
        CountDownLatch latch = new CountDownLatch(1);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    service.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        
        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        
        int totalWolves = island.getCell(0, 0).getAnimalCount() + island.getCell(1, 0).getAnimalCount();
        
        assertEquals(1, totalWolves, "Animal should not be duplicated during movement");
    }
}
