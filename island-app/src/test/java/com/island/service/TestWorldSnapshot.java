package com.island.service;

import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;

@Getter
@Setter
@Builder
public class TestWorldSnapshot implements WorldSnapshot {
    private int tickCount;
    private int width;
    private int height;
    private int totalEntityCount;
    private String simulationType = "test";

    @Override
    public int getTickCount() { return tickCount; }
    @Override
    public int getWidth() { return width; }
    @Override
    public int getHeight() { return height; }
    @Override
    public int getTotalEntityCount() { return totalEntityCount; }

    @Override
    public Map<String, Number> getMetrics() {
        return Collections.emptyMap();
    }

    @Override
    public NodeSnapshot getNodeSnapshot(int x, int y) {
        return null;
    }
}
