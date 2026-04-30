package com.island.model;

import com.island.engine.NodeSnapshot;
import com.island.engine.WorldSnapshot;
import com.island.content.DeathCause;
import java.util.HashMap;
import java.util.Map;

/**
 * Island-specific implementation of WorldSnapshot.
 */
public class IslandSnapshot implements WorldSnapshot {
    private final Island island;
    private final int tickCount;

    public IslandSnapshot(Island island) {
        this.island = island;
        this.tickCount = island.getTickCount();
    }

    @Override
    public int getTickCount() {
        return tickCount;
    }

    @Override
    public int getWidth() {
        return island.getWidth();
    }

    @Override
    public int getHeight() {
        return island.getHeight();
    }

    @Override
    public int getTotalOrganismCount() {
        return island.getTotalOrganismCount();
    }

    @Override
    public double getGlobalSatiety() {
        return island.getStatisticsService().calculateGlobalSatiety(island);
    }

    @Override
    public int getStarvingCount() {
        return island.getStatisticsService().calculateStarvingCount(island);
    }

    @Override
    public Map<String, Integer> getSpeciesCounts() {
        Map<String, Integer> counts = new HashMap<>();
        island.getSpeciesCounts().forEach((k, v) -> counts.put(k.getCode(), v));
        return counts;
    }

    @Override
    public Map<String, Integer> getDeathStatsBySpecies(String causeCode) {
        try {
            DeathCause cause = DeathCause.valueOf(causeCode);
            Map<String, Integer> stats = new HashMap<>();
            island.getTotalDeathsBySpecies(cause).forEach((k, v) -> stats.put(k.getCode(), v));
            return stats;
        } catch (IllegalArgumentException e) {
            return java.util.Collections.emptyMap();
        }
    }

    @Override
    public int getTotalDeathCount(String causeCode) {
        try {
            DeathCause cause = DeathCause.valueOf(causeCode);
            return island.getTotalAnimalDeathCount(cause);
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return new CellSnapshot(island.getCell(x, y));
    }
}
