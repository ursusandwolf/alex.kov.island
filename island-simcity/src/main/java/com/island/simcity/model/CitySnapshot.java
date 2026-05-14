package com.island.simcity.model;

import java.util.HashMap;
import java.util.Map;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
public class CitySnapshot implements WorldSnapshot {
    private int width;
    private int height;
    private int totalEntityCount;
    private int tickCount;
    private Map<String, Number> metrics;
    private CityNodeSnapshot[][] nodes;

    public CitySnapshot() {
        // No-args constructor for Jackson
    }

    public CitySnapshot(CityMap map, int tickCount) {
        this.width = map.getWidth();
        this.height = map.getHeight();
        this.totalEntityCount = map.getPopulation();
        this.tickCount = tickCount;
        this.metrics = calculateMetrics(map);
        
        this.nodes = new CityNodeSnapshot[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                this.nodes[x][y] = new CityNodeSnapshot(map.getGrid()[x][y]);
            }
        }
    }

    private Map<String, Number> calculateMetrics(CityMap map) {
        Map<String, Number> metrics = new HashMap<>();
        metrics.put("money", map.getMoney());
        metrics.put("resDemand", map.getResDemand());
        metrics.put("comDemand", map.getComDemand());
        metrics.put("indDemand", map.getIndDemand());
        metrics.put("totalJobs", map.getTotalJobs());
        metrics.put("averageEQ", map.getAverageEQ());
        metrics.put("averageHealth", map.getAverageHealth());
        metrics.put("income", map.getLastTickIncome());
        metrics.put("expenses", map.getLastTickExpenses());
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

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return nodes[x][y];
    }
}