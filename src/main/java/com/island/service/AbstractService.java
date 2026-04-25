package com.island.service;

import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Base class for services that process the island cell by cell in parallel.
 */
public abstract class AbstractService implements Runnable {
    protected final Island island;
    protected final ExecutorService executor;

    protected AbstractService(Island island, ExecutorService executor) {
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

    protected abstract void processCell(Cell cell);
}
