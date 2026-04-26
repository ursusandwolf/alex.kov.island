package com.island.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Composite: Chunk consists of cells.
 */
public class Chunk {
    private final int chunkIdX, chunkIdY;
    private final int startX, endX, startY, endY;
    private final Island island;
    private final List<Cell> cells = new ArrayList<>();

    public Chunk(int idX, int idY, int sx, int ex, int sy, int ey, Island island) {
        this.chunkIdX = idX;
        this.chunkIdY = idY;
        this.startX = sx;
        this.endX = ex;
        this.startY = sy;
        this.endY = ey;
        this.island = island;
        initializeCells();
    }

    private void initializeCells() {
        for (int x = startX; x < endX; x++) {
            for (int y = startY; y < endY; y++) {
                cells.add(island.getGrid()[x][y]);
            }
        }
    }

    public List<Cell> getCells() { return cells; }
    public int getChunkIdX() { return chunkIdX; }
    public int getChunkIdY() { return chunkIdY; }

    @Override
    public String toString() {
        return String.format("Chunk[%d,%d] клеток=%d", chunkIdX, chunkIdY, cells.size());
    }
}
