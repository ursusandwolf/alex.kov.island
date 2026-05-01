package com.island.simcity.model;

import com.island.engine.NodeSnapshot;
import com.island.engine.WorldSnapshot;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CitySnapshot implements WorldSnapshot {
    private final CityMap map;

    @Override
    public int getTickCount() {
        return 0;
    }

    @Override
    public int getWidth() {
        return map.getWidth();
    }

    @Override
    public int getHeight() {
        return map.getHeight();
    }

    @Override
    public int getTotalOrganismCount() {
        return map.getPopulation();
    }

    @Override
    public double getGlobalSatiety() {
        return 1.0;
    }

    @Override
    public int getStarvingCount() {
        return 0;
    }

    @Override
    public Map<String, Integer> getSpeciesCounts() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Integer> getDeathStatsBySpecies(String causeCode) {
        return Collections.emptyMap();
    }

    @Override
    public int getTotalDeathCount(String causeCode) {
        return 0;
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return null;
    }
}
