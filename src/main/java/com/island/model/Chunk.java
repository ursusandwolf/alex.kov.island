package com.island.model;

import com.island.content.Animal;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

// Composite: Chunk состоит из ячеек
// Command: Chunk может быть задачей для потока
@Getter
public class Chunk {
    private final int chunkIdX, chunkIdY, startX, endX, startY, endY;
    private final Island island;
    private final List<Cell> cells = new ArrayList<>();

    public Chunk(int idX, int idY, int sX, int eX, int sY, int eY, Island island) {
        this.chunkIdX = idX; this.chunkIdY = idY;
        this.startX = sX; this.endX = eX;
        this.startY = sY; this.endY = eY;
        this.island = island;
        initializeCells();
    }

    private void initializeCells() {
        for (int x = startX; x < endX; x++)
            for (int y = startY; y < endY; y++) {
                Cell cell = island.getCellUnsafe(x, y);
                if (cell != null) cells.add(cell);
            }
    }

    public void processTick() {
        // TODO: Реализация 4 фаз симуляции
    }

    public int getCellCount() { return cells.size(); }

    @Override
    public String toString() {
        return String.format("Chunk[%d,%d] клеток=%d", chunkIdX, chunkIdY, cells.size());
    }
}
