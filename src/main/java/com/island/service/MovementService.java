package com.island.service;
import com.island.util.RandomUtils;
import com.island.content.Animal;
import com.island.engine.GameLoop;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.model.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;


public class MovementService implements Runnable {
    private final Island island;
    private final ExecutorService executor;

    public MovementService(Island island, ExecutorService executor) {
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
            if (!animal.isAlive()) continue;
            
            int speed = animal.getSpeed();
            if (speed > 0) {
                int dx = RandomUtils.nextInt(-speed, speed + 1);
                int dy = RandomUtils.nextInt(-speed, speed + 1);
                int tx = cell.getX() + dx;
                int ty = cell.getY() + dy;
                
                Cell target = island.getCell(tx, ty);
                if (target != cell) {
                    island.moveOrganism(animal, cell, target);
                }
            }
        }
    }
}
