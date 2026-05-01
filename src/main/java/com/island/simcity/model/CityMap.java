package com.island.simcity.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.WorldSnapshot;
import com.island.simcity.entities.SimEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

@Getter
public class CityMap implements SimulationWorld<SimEntity> {
    private final int width;
    private final int height;
    private final CityTile[][] grid;
    private long money = 10000;
    private int population = 0;

    public CityMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new CityTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new CityTile(x, y, this);
            }
        }
    }

    public void addMoney(long amount) {
        this.money += amount;
    }

    public void setPopulation(int population) {
        this.population = population;
    }

    @Override
    public Collection<? extends Collection<? extends SimulationNode<SimEntity>>> getParallelWorkUnits() {
        // For simplicity, one unit containing all nodes (not truly parallel but valid)
        // Or we can chunk it if needed. Let's do a simple chunking.
        List<List<CityTile>> chunks = new ArrayList<>();
        List<CityTile> currentChunk = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                currentChunk.add(grid[x][y]);
                if (currentChunk.size() >= 10) {
                    chunks.add(currentChunk);
                    currentChunk = new ArrayList<>();
                }
            }
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }
        return chunks;
    }

    @Override
    public Optional<SimulationNode<SimEntity>> getNode(SimulationNode<SimEntity> current, int dx, int dy) {
        if (current instanceof CityTile tile) {
            int tx = tile.getX() + dx;
            int ty = tile.getY() + dy;
            if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
                return Optional.of(grid[tx][ty]);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean moveEntity(SimEntity entity, SimulationNode<SimEntity> from, SimulationNode<SimEntity> to) {
        if (to.canAccept(entity)) {
            if (from.removeEntity(entity)) {
                return to.addEntity(entity);
            }
        }
        return false;
    }

    @Override
    public WorldSnapshot createSnapshot() {
        return new CitySnapshot(this);
    }

    @Override
    public void tick(int tickCount) {
        // Global logic per tick
    }
}
