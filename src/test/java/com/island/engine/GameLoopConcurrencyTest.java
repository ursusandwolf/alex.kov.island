package com.island.engine;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameLoopConcurrencyTest {
    @Test
    void shouldNotThrowExceptionWhenAddingTaskFromAnotherThread() throws InterruptedException {
        GameLoop<Mortal> gameLoop = new GameLoop<>(1, 2); // Very fast ticks
        CountDownLatch startLatch = new CountDownLatch(1);
        
        gameLoop.addRecurringTask(() -> {
            startLatch.countDown();
            try {
                Thread.sleep(10); // Hold the loop thread
            } catch (InterruptedException e) {}
        });

        gameLoop.start();
        try {
            assertTrue(startLatch.await(2, TimeUnit.SECONDS), "Loop should have started");
            
            // Now spam addRecurringTask from multiple threads
            int threadCount = 10;
            CountDownLatch finishLatch = new CountDownLatch(threadCount);
            for (int i = 0; i < threadCount; i++) {
                new Thread(() -> {
                    for (int j = 0; j < 100; j++) {
                        gameLoop.addRecurringTask(() -> {});
                    }
                    finishLatch.countDown();
                }).start();
            }
            
            assertTrue(finishLatch.await(5, TimeUnit.SECONDS), "Spamming threads should finish");
            Thread.sleep(100); 
            assertTrue(gameLoop.isRunning(), "Game loop should still be running");
        } finally {
            gameLoop.stop();
        }
    }
}
