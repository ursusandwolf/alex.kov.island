package com.island.simcity.model;

import com.island.engine.NodeSnapshot;
import com.island.engine.WorldSnapshot;
import java.util.HashMap;
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
    public int getTotalEntityCount() {
        return map.getPopulation();
    }

    @Override
    public Map<String, Number> getMetrics() {
        Map<String, Number> metrics = new HashMap<>();
        metrics.put("money", map.getMoney());
        metrics.put("resDemand", map.getResDemand());
        metrics.put("comDemand", map.getComDemand());
        metrics.put("indDemand", map.getIndDemand());
        metrics.put("totalJobs", map.getTotalJobs());
        return metrics;
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return null;
    }
}
