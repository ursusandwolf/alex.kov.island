package com.island.engine;

import java.util.ArrayList;
import java.util.List;

public class GameLoop implements Runnable {
    private final List<Runnable> recurringTasks = new ArrayList<>();
    private volatile boolean running = false;

    public void addRecurringTask(Runnable task) {
        recurringTasks.add(task);
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            tick();
            try {
                Thread.sleep(1000); // Базовый такт
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void tick() {
        recurringTasks.forEach(Runnable::run);
    }
}
