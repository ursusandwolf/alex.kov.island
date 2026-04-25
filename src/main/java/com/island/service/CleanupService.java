package com.island.service;
import com.island.content.plants.*;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class CleanupService implements Runnable {
    private final Island island;
    private final ExecutorService executor;

    public CleanupService(Island island, ExecutorService executor) {
        this.island = island;
        this.executor = executor;
    }

    @Override
    public void run() {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    cell.cleanupDeadOrganisms();
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
}
