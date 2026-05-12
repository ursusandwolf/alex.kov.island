package com.island.nature.model;

import java.util.HashMap;
import java.util.Map;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;
import com.island.nature.entities.core.DeathCause;

/**
 * Island-specific implementation of WorldSnapshot.
 */
public class IslandSnapshot implements WorldSnapshot {
    private final int tickCount;
    private final int width;
    private final int height;
    private final int totalEntityCount;
    private final Map<String, Number> metrics;
    private final CellSnapshot[][] nodes;

    public IslandSnapshot(Island island) {
        this.tickCount = island.getTickCount();
        this.width = island.getWidth();
        this.height = island.getHeight();
        this.totalEntityCount = island.getTotalOrganismCount();
        this.metrics = Map.copyOf(calculateMetrics(island));
        
        this.nodes = new CellSnapshot[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.nodes[x][y] = new CellSnapshot(island.getCell(x, y));
            }
        }
    }

    private Map<String, Number> calculateMetrics(Island island) {
        Map<String, Number> metrics = new HashMap<>();
        metrics.put("globalSatiety", island.getStatisticsService().calculateGlobalSatiety(island));
        metrics.put("hungryCount", island.getStatisticsService().calculateHungryCount(island));
        
        island.getSpeciesCounts().forEach((k, v) -> metrics.put("species." + k.getCode(), v));
        
        for (DeathCause cause : DeathCause.values()) {
            metrics.put("deaths." + cause.name(), (double) island.getTotalAnimalDeathCount(cause));
            island.getTotalDeathsBySpecies(cause).forEach((k, v) ->
                    metrics.put("deaths." + cause.name() + "." + k.getCode(), v));
        }
        return metrics;
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getTotalEntityCount() {
        return totalEntityCount;
    }

    @Override
    public Map<String, Number> getMetrics() {
        return metrics;
    }

    public CellSnapshot[][] getNodes() {
        return nodes;
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return nodes[x][y];
    }
}