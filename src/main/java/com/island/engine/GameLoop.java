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
        // Use virtual threads for better scalability with many tasks
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
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
        
        // Даем текущему такту шанс завершиться перед закрытием taskExecutor
        try {
            if (!scheduler.awaitTermination(2, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        taskExecutor.shutdown();
        try {
            if (!taskExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                taskExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            taskExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public void runTick() {
        executeTick();
    }

    private void tick() {
        if (!running || taskExecutor.isShutdown()) return;
        executeTick();
    }

    private void executeTick() {
        try {
            // Execute each major phase sequentially to maintain simulation logic,
            // but internal phase can be parallelized (e.g. movement by chunks)
            for (Runnable task : recurringTasks) {
                if (taskExecutor.isShutdown()) break;
                task.run();
            }
        } catch (Exception e) {
            if (!taskExecutor.isShutdown()) {
                System.err.println("Ошибка во время такта симуляции: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }
}
