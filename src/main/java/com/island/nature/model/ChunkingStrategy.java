package com.island.nature.model;

import java.util.List;

/**
 * Strategy for partitioning the island into chunks for parallel processing.
 */
public interface ChunkingStrategy {
    List<Chunk> partition(int width, int height, Island island);
}
