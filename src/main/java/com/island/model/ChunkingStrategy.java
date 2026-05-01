package com.island.model;

import com.island.content.Organism;
import com.island.engine.SimulationNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for partitioning the island into chunks for parallel processing.
 */
public class ChunkingStrategy {
    public List<Chunk> partition(int width, int height, Island island) {
        List<Chunk> chunks = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors();
        int totalCells = width * height;

        int targetTasks;
        if (totalCells <= 64) {
            targetTasks = 16;
        } else if (totalCells <= processors * 16) {
            targetTasks = processors * 2;
        } else {
            targetTasks = processors * 4;
        }

        targetTasks = Math.min(targetTasks, totalCells);
        int cellsPerChunk = Math.max(1, totalCells / targetTasks);
        int chunkSize = (int) Math.sqrt(cellsPerChunk);

        if (chunkSize < 1) {
            chunkSize = 1;
        }
        if (totalCells > 1000 && chunkSize > 32) {
            chunkSize = 32;
        }

        for (int x = 0; x < width; x += chunkSize) {
            for (int y = 0; y < height; y += chunkSize) {
                chunks.add(new Chunk(x / chunkSize, y / chunkSize,
                        x, Math.min(x + chunkSize, width),
                        y, Math.min(y + chunkSize, height), island));
            }
        }
        return chunks;
    }
}