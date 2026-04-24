package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GameLoop {
    private final List<Runnable> recurringTasks = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final ExecutorService taskExecutor;
    private final int tickDurationMs;
    private ScheduledFuture<?> timerHandle;
    private volatile boolean running = false;

    public GameLoop(int tickDurationMs) {
        this.tickDurationMs = tickDurationMs;
        // Use available processors for parallel task execution
        this.taskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void addRecurringTask(Runnable task) {
        recurringTasks.add(task);
    }

    public void start() {
        running = true;
        timerHandle = scheduler.scheduleAtFixedRate(this::tick, 0, tickDurationMs, TimeUnit.MILLISECONDS);
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        running = false;
        if (timerHandle != null) {
            timerHandle.cancel(false);
        }
        scheduler.shutdown();
        taskExecutor.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) scheduler.shutdownNow();
            if (!taskExecutor.awaitTermination(1, TimeUnit.SECONDS)) taskExecutor.shutdownNow();
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            taskExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void tick() {
        if (!running) return;
        try {
            // Execute each major phase sequentially to maintain simulation logic,
            // but internal phase can be parallelized (e.g. movement by chunks)
            for (Runnable task : recurringTasks) {
                task.run();
            }
        } catch (Exception e) {
            System.err.println("Ошибка во время такта симуляции: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }
}
