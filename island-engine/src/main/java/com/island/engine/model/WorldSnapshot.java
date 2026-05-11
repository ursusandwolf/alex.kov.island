package com.island.engine.model;

import com.island.engine.core.EngineAPI;
import java.util.Map;

/**
 * Domain-agnostic snapshot of the simulation world state.
 */
@EngineAPI
public interface WorldSnapshot {
    /**
     * Gets the current tick count of the simulation when the snapshot was taken.
     */
    int getTickCount();

    /**
     * Gets the width of the world.
     */
    int getWidth();

    /**
     * Gets the height of the world.
     */
    int getHeight();

    /**
     * Gets the total number of entities currently in the world.
     */
    int getTotalEntityCount();

    /**
     * Gets domain-specific metrics (e.g., population by species, happiness, etc.).
     */
    Map<String, Number> getMetrics();

    /**
     * Gets a snapshot of a specific node at the given coordinates.
     */
    NodeSnapshot getNodeSnapshot(int x, int y);
}