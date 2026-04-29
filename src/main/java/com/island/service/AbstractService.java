package com.island.service;

import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Base class for all simulation services.
 */
public abstract class AbstractService implements Runnable {
    private final Island island;
    private final ExecutorService executor;
    private final RandomProvider random;

    protected AbstractService(Island island, ExecutorService executor, RandomProvider random) {
        this.island = island;
        this.executor = executor;
        this.random = random;
    }

    public Island getIsland() {
        return island;
    }

    protected RandomProvider getRandom() {
        return random;
    }

    @Override
    public void run() {
        if (executor.isShutdown()) {
            return;
        }

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
            if (!executor.isShutdown()) {
                executor.invokeAll(tasks);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Silently ignore if shutdown happened between check and execution
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected abstract void processCell(Cell cell);
}
