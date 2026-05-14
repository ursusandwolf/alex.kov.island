package com.island.nature.model;

import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Advanced implementation of ChunkingStrategy that adapts chunk sizes based on entity density.
 * Uses a recursive splitting approach to balance load across chunks.
 */
@Slf4j
public class DynamicChunkingStrategy implements ChunkingStrategy {
    private final Configuration config;

    public DynamicChunkingStrategy(Configuration config) {
        this.config = config;
    }

    @Override
    public List<Chunk> partition(int width, int height, Island island) {
        List<Chunk> chunks = new ArrayList<>();
        splitRecursively(0, width, 0, height, island, chunks, 0);
        log.debug("Dynamic partitioning complete: {} chunks created", chunks.size());
        return chunks;
    }

    private void splitRecursively(int sx, int ex, int sy, int ey, Island island, List<Chunk> chunks, int depth) {
        int w = ex - sx;
        int h = ey - sy;
        
        if (w <= 0 || h <= 0) {
            return;
        }

        long executionTime = getLastExecutionTime(sx, ex, sy, ey, island);
        int entities = countEntities(sx, ex, sy, ey, island);
        
        int targetLoad = config.getDynamicChunkingTargetLoad();
        long targetTimeNanos = config.getTickDurationMs() * 1_000_000L / Runtime.getRuntime().availableProcessors() / 2; // Target 50% of tick time per thread
        int minSize = config.getDynamicChunkingMinSize();

        // Split if too many entities OR high execution time, and the area is large enough to split
        boolean shouldSplit = (entities > targetLoad || executionTime > targetTimeNanos) 
                && (w > minSize || h > minSize) && depth < 5;

        if (shouldSplit) {
            int midX = sx + w / 2;
            int midY = sy + h / 2;

            if (w > minSize && h > minSize) {
                // Split into 4 quadrants
                splitRecursively(sx, midX, sy, midY, island, chunks, depth + 1);
                splitRecursively(midX, ex, sy, midY, island, chunks, depth + 1);
                splitRecursively(sx, midX, midY, ey, island, chunks, depth + 1);
                splitRecursively(midX, ex, midY, ey, island, chunks, depth + 1);
            } else if (w > minSize) {
                // Split vertically
                splitRecursively(sx, midX, sy, ey, island, chunks, depth + 1);
                splitRecursively(midX, ex, sy, ey, island, chunks, depth + 1);
            } else {
                // Split horizontally
                splitRecursively(sx, ex, sy, midY, island, chunks, depth + 1);
                splitRecursively(sx, ex, midY, ey, island, chunks, depth + 1);
            }
        } else {
            // Create a chunk for this region
            chunks.add(new Chunk(chunks.size(), depth, sx, ex, sy, ey, island));
        }
    }

    private long getLastExecutionTime(int sx, int ex, int sy, int ey, Island island) {
        long totalTime = 0;
        // Find existing chunks that overlap this region
        for (Chunk chunk : island.getChunks()) {
            if (chunk.getStartX() >= sx && chunk.getEndX() <= ex && 
                chunk.getStartY() >= sy && chunk.getEndY() <= ey) {
                totalTime += chunk.getLastExecutionTimeNanos();
            }
        }
        return totalTime;
    }

    private int countEntities(int sx, int ex, int sy, int ey, Island island) {
        int count = 0;
        Cell[][] grid = island.getGrid();
        for (int x = sx; x < ex; x++) {
            for (int y = sy; y < ey; y++) {
                count += grid[x][y].getEntityCount();
            }
        }
        return count;
    }
}
