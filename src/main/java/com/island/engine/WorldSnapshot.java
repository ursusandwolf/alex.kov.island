package com.island.engine;

import java.util.Map;

/**
 * Domain-agnostic snapshot of the simulation world state.
 */
public interface WorldSnapshot {
    int getTickCount();

    int getWidth();

    int getHeight();

    /**
     * Total number of entities in the world.
     */
    int getTotalEntityCount();

    /**
     * Gets domain-specific metrics (e.g., population by species, happiness, etc.).
     */
    Map<String, Number> getMetrics();

    /**
     * Gets snapshot of a specific node.
     */
    NodeSnapshot getNodeSnapshot(int x, int y);
}
