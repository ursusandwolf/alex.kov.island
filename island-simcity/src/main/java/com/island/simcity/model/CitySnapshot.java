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
    private CityNodeSnapshot[][] nodes;

    private synchronized void ensureNodes() {
        if (nodes == null) {
            int w = getWidth();
            int h = getHeight();
            nodes = new CityNodeSnapshot[w][h];
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    nodes[x][y] = new CityNodeSnapshot(map.getGrid()[x][y]);
                }
            }
        }
    }

    public CityNodeSnapshot[][] getNodes() {
        ensureNodes();
        return nodes;
    }

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