package com.island.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameLoop {
    private final List<Runnable> recurringTasks = new ArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final int tickDurationMs;

    public GameLoop(int tickDurationMs) {
        this.tickDurationMs = tickDurationMs;
    }

    public void addRecurringTask(Runnable task) {
        recurringTasks.add(task);
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::tick, 0, tickDurationMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    private void tick() {
        try {
            recurringTasks.forEach(Runnable::run);
        } catch (Exception e) {
            System.err.println("Ошибка во время такта симуляции: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
