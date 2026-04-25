package com.island.model;
import com.island.util.RandomUtils;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
import lombok.Getter;
import java.util.*;


// Composite: Остров состоит из ячеек
// Factory: Создание организмов при инициализации
@Getter
public class Island {
    private final int width, height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private int tickCount = 0;

    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
    }

    public void nextTick() {
        tickCount++;
    }

    public int getTickCount() {
        return tickCount;
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = new Cell(x, y);
    }

    private void partitionIntoChunks() {
        // Split island into 4 chunks (2x2 grid)
        int midX = width / 2;
        int midY = height / 2;

        if (midX > 0 && midY > 0) {
            chunks.add(new Chunk(0, 0, 0, midX, 0, midY, this));
            chunks.add(new Chunk(1, 0, midX, width, 0, midY, this));
            chunks.add(new Chunk(0, 1, 0, midX, midY, height, this));
            chunks.add(new Chunk(1, 1, midX, width, midY, height, this));
        } else if (midX > 0) {
            chunks.add(new Chunk(0, 0, 0, midX, 0, height, this));
            chunks.add(new Chunk(1, 0, midX, width, 0, height, this));
        } else {
            chunks.add(new Chunk(0, 0, 0, width, 0, height, this));
        }
    }

    public Cell getCell(int x, int y) {
        int wx = ((x % width) + width) % width;
        int wy = ((y % height) + height) % height;
        return grid[wx][wy];
    }

    public Cell getCellUnsafe(int x, int y) {
        return (x < 0 || x >= width || y < 0 || y >= height) ? null : grid[x][y];
    }

    public boolean moveOrganism(Animal animal, Cell from, Cell to) {
        if (from == to) return true;

        // Order locks by System.identityHashCode or coordinates to prevent deadlocks
        Cell first = (System.identityHashCode(from) < System.identityHashCode(to)) ? from : to;
        Cell second = (first == from) ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (from.getAnimals().contains(animal)) {
                    if (to.addAnimal(animal)) {
                        from.removeAnimal(animal);
                        return true;
                    }
                }
                return false;
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    public void initializeWorld(SpeciesConfig config) {
        // TODO: Инициализация мира
    }

    public Map<String, Integer> getSpeciesCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Animal a : grid[x][y].getAnimals()) {
                    if (a.isAlive()) {
                        counts.put(a.getSpeciesKey(), counts.getOrDefault(a.getSpeciesKey(), 0) + 1);
                    }
                }
            }
        }
        return counts;
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
