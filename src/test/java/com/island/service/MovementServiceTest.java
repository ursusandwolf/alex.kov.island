package com.island.service;

import com.island.content.animals.predators.Wolf;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementServiceTest {

    @Test
    void testConcurrentMovement() throws InterruptedException {
        // This test attempts to trigger the race condition where an animal might end up in two cells
        Island island = new Island(2, 1);
        Cell cell0 = island.getCell(0, 0);
        Cell cell1 = island.getCell(1, 0);
        
        Wolf wolf = new Wolf();
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
        
        // If the race condition exists, totalWolves might be > 1
        // Because multiple threads might successfully target.addAnimal(wolf) before cell.removeAnimal(wolf) is called by any of them
        // or before it's reflected in other threads' view.
        
        // Note: MovementService.run() iterates over all cells. 
        // In our case, cell0 and cell1.
        
        System.out.println("Total wolves after concurrent movement: " + totalWolves);
        // We expect only 1 wolf to exist in the world
        assertEquals(1, totalWolves, "Animal should not be duplicated during movement");
    }
}
