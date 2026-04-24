package com.island.service;

import com.island.content.Animal;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class LifecycleService implements Runnable {
    private final Island island;
    private final ExecutorService executor;

    public LifecycleService(Island island, ExecutorService executor) {
        this.island = island;
        this.executor = executor;
    }

    @Override
    public void run() {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    processCell(cell);
                }
                return null;
            });
        }
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processCell(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                animal.setHiding(false); // Reset protection flag
                animal.checkState();
            }
        }
    }
}
