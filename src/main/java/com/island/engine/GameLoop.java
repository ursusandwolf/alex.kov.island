package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Orchestrates the simulation ticks.
 */
public class GameLoop {
    private final List<Runnable> recurringTasks = new ArrayList<>();
    private final ScheduledExecutorService timer;
    private final ExecutorService taskExecutor;
    private final int tickDurationMs;
    private ScheduledFuture<?> timerHandle;
    private volatile boolean running = false;

    public GameLoop(int tickDurationMs) {
        this.tickDurationMs = tickDurationMs;
        // Use virtual threads for better scalability with many tasks
        this.taskExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.timer = Executors.newSingleThreadScheduledExecutor();
    }

    public void addRecurringTask(Runnable task) {
        recurringTasks.add(task);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        timerHandle = timer.scheduleAtFixedRate(this::tick, 0, tickDurationMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stop() {
        if (!running) return;
        running = false;
        if (timerHandle != null) timerHandle.cancel(false);
        timer.shutdown();
        taskExecutor.shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    public void runTick() {
        tick();
    }

    private void tick() {
        for (Runnable task : recurringTasks) {
            if (!running) break; // Stop immediately if gameLoop was stopped
            try {
                task.run();
            } catch (Exception e) {
                if (running) { // Only log errors if we're supposed to be running
                    System.err.println("Ошибка во время такта симуляции: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }
}
