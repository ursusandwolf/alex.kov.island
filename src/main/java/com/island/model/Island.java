package com.island.model;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;
import lombok.Getter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Composite: Остров состоит из ячеек
// Factory: Создание организмов при инициализации
@Getter
public class Island {
    private final int width, height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();

    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        initializeGrid();
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = new Cell(x, y);
    }

    public Cell getCell(int x, int y) {
        int wx = ((x % width) + width) % width;
        int wy = ((y % height) + height) % height;
        return grid[wx][wy];
    }

    public Cell getCellUnsafe(int x, int y) {
        return (x < 0 || x >= width || y < 0 || y >= height) ? null : grid[x][y];
    }

    public boolean moveOrganism(Animal a, int fx, int fy, int tx, int ty) {
        // TODO: Реализация потокобезопасного перемещения
        return false;
    }

    public void initializeWorld(SpeciesConfig config) {
        // TODO: Инициализация мира
    }

    public int getTotalOrganismCount() {
        int count = 0;
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                count += grid[x][y].getAnimalCount() + grid[x][y].getPlantCount();
        return count;
    }

    public String getStatistics() {
        return String.format("Остров[%dx%d] Всего организмов: %d", width, height, getTotalOrganismCount());
    }
}
