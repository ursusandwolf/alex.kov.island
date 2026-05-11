package com.island.simcity.model;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;

@RequiredArgsConstructor
public class CitySnapshot implements WorldSnapshot {
    private final CityMap map;
    private final int tickCount;

    @Override
    public int getTickCount() {
        return tickCount;
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
        metrics.put("income", map.getLastTickIncome());
        metrics.put("expenses", map.getLastTickExpenses());
        return metrics;
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return new CityNodeSnapshot(map.getGrid()[x][y]);
    }
}