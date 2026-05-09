package com.island.engine;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.core.ExecutionMode;
import com.island.engine.model.Mortal;
import com.island.engine.model.Tickable;
import com.island.engine.internal.ParallelDispatcher;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.Phase;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.scheduling.ScheduledTask;

public class GameLoopConcurrencyTest {
    @Test
    void shouldExecuteTasksInPriorityOrder() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(100, executor, scheduler);
        List<Integer> executionOrder = new CopyOnWriteArrayList<>();

        // Low priority task (50)
        gameLoop.addRecurringTask(new ScheduledTask() {
            @Override public Phase phase() { return Phase.SIMULATION; }
            @Override public int priority() { return 50; }
            @Override public ExecutionMode executionMode() { return ExecutionMode.SEQUENTIAL; }
            @Override public void tick(int tickCount) { executionOrder.add(50); }
        });

        // High priority task (100)
        gameLoop.addRecurringTask(new ScheduledTask() {
            @Override public Phase phase() { return Phase.SIMULATION; }
            @Override public int priority() { return 100; }
            @Override public ExecutionMode executionMode() { return ExecutionMode.SEQUENTIAL; }
            @Override public void tick(int tickCount) { executionOrder.add(100); }
        });

        // Very low priority task (10)
        gameLoop.addRecurringTask(new ScheduledTask() {
            @Override public Phase phase() { return Phase.SIMULATION; }
            @Override public int priority() { return 10; }
            @Override public ExecutionMode executionMode() { return ExecutionMode.SEQUENTIAL; }
            @Override public void tick(int tickCount) { executionOrder.add(10); }
        });

        gameLoop.runTick();

        assertEquals(List.of(100, 50, 10), executionOrder, "Tasks should execute in descending priority order");
    }

    @Test
    void shouldExecutePreparePhaseBeforeSimulation() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(100, executor, scheduler);
        List<String> phases = new CopyOnWriteArrayList<>();
        gameLoop.addRecurringTask(new ScheduledTask() {
            @Override public Phase phase() { return Phase.PREPARE; }
            @Override public int priority() { return 50; }
            @Override public void tick(int tc) { phases.add("PREPARE"); }
        });
        gameLoop.addRecurringTask(new ScheduledTask() {
            @Override public Phase phase() { return Phase.SIMULATION; }
            @Override public int priority() { return 50; }
            @Override public void tick(int tc) { phases.add("SIMULATION"); }
        });
        gameLoop.runTick();
        assertEquals(List.of("PREPARE", "SIMULATION"), phases);
    }

    @Test
    void shouldContinueAfterServiceException() {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(100, executor, scheduler);
        AtomicBoolean secondRan = new AtomicBoolean(false);
        gameLoop.addRecurringTask((Tickable) tc -> { throw new RuntimeException("boom"); });
        gameLoop.addRecurringTask((Tickable) tc -> secondRan.set(true));
        gameLoop.runTick();
        assertTrue(secondRan.get(), "Second task should run even if first one failed");
    }

    @Test
    void shouldNotThrowExceptionWhenAddingTaskFromAnotherThread() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(1, executor, scheduler); // Very fast ticks
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