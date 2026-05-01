package com.island.model;

import com.island.content.DeathCause;
import com.island.engine.NodeSnapshot;
import com.island.engine.WorldSnapshot;
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
    public int getTotalEntityCount() {
        return island.getTotalOrganismCount();
    }

    @Override
    public Map<String, Number> getMetrics() {
        Map<String, Number> metrics = new HashMap<>();
        metrics.put("globalSatiety", island.getStatisticsService().calculateGlobalSatiety(island));
        metrics.put("starvingCount", island.getStatisticsService().calculateStarvingCount(island));
        
        // Add species counts as metrics with prefix
        island.getSpeciesCounts().forEach((k, v) -> metrics.put("species." + k.getCode(), v));
        
        // Add death stats
        for (DeathCause cause : DeathCause.values()) {
            metrics.put("deaths." + cause.name(), (double) island.getTotalAnimalDeathCount(cause));
            island.getTotalDeathsBySpecies(cause).forEach((k, v) ->
                    metrics.put("deaths." + cause.name() + "." + k.getCode(), v));
        }
        
        return metrics;
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return new CellSnapshot(island.getCell(x, y));
    }
}
