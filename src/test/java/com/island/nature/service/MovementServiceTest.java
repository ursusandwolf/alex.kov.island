package com.island.nature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.island.nature.config.Configuration;
import com.island.nature.entities.GenericAnimal;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.nature.model.Island;
import com.island.util.DefaultRandomProvider;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MovementServiceTest {
    private final Configuration config = new Configuration();
    private final SpeciesRegistry registry = new SpeciesLoader(config).load();

    @Test
    void testConcurrentMovement() throws InterruptedException {
        Island island = new Island(config, 2, 1, registry, new StatisticsService(config));
        Cell cell0 = island.getCell(0, 0);
        
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        cell0.addAnimal(wolf);
        
        int numThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        MovementService service = new MovementService(island, registry, executor, new DefaultRandomProvider());
        
        CountDownLatch latch = new CountDownLatch(1);
        
        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    service.tick(1);
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
