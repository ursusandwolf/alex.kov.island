package com.island.nature.model;

import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of ChunkingStrategy that partitions the island into equal-sized chunks.
 * Uses configuration parameters to determine chunk size.
 */
public class StaticChunkingStrategy implements ChunkingStrategy {
    private final Configuration config;

    public StaticChunkingStrategy(Configuration config) {
        this.config = config;
    }

    @Override
    public List<Chunk> partition(int width, int height, Island island) {
        List<Chunk> chunks = new ArrayList<>();
        int processors = Runtime.getRuntime().availableProcessors();
        int totalCells = width * height;

        int targetTasks;
        if (totalCells <= config.getPartitioningSmallWorldThreshold()) {
            targetTasks = config.getPartitioningSmallWorldTasks();
        } else if (totalCells <= processors * config.getPartitioningMediumWorldMultiplier()) {
            targetTasks = processors * config.getPartitioningMediumWorldTasksMultiplier();
        } else {
            targetTasks = processors * config.getPartitioningLargeWorldTasksMultiplier();
        }

        targetTasks = Math.min(targetTasks, totalCells);
        int cellsPerChunk = Math.max(1, totalCells / targetTasks);
        int chunkSize = (int) Math.sqrt(cellsPerChunk);

        if (chunkSize < 1) {
            chunkSize = 1;
        }

        if (totalCells > config.getPartitioningLargeWorldThreshold() && chunkSize > config.getPartitioningMaxChunkSize()) {
            chunkSize = config.getPartitioningMaxChunkSize();
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
