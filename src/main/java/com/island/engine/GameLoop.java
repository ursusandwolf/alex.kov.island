package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the simulation ticks.
 */
public class GameLoop {
    private final List<Tickable> recurringTasks = new ArrayList<>();
    private final long tickDurationMs;
    private final ExecutorService taskExecutor;
    private volatile boolean running = false;
    private int tickCount = 0;

    public GameLoop(long tickDurationMs) {
        this.tickDurationMs = tickDurationMs;
        this.taskExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void addRecurringTask(Tickable task) {
        recurringTasks.add(task);
    }

    public void addRecurringTask(Runnable runnable) {
        recurringTasks.add(t -> runnable.run());
    }

    public void start() {
        if (running) {
            return;
        }
        running = true;
        new Thread(this::run).start();
    }

    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        taskExecutor.shutdown();
    }

    public boolean isRunning() {
        return running;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void runTick() {
        tickCount++;
        for (Tickable task : recurringTasks) {
            if (!running) {
                break;
            }
            try {
                task.tick(tickCount);
            } catch (Exception e) {
                System.err.println("Ошибка во время такта симуляции: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void run() {
        while (running) {
            long startTime = System.currentTimeMillis();
            runTick();
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = tickDurationMs - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public ExecutorService getTaskExecutor() {
        return taskExecutor;
    }
}
