package com.island.engine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a spatial unit in the simulation where entities reside.
 */
public interface SimulationNode {
    /**
     * Gets the synchronization lock for this node.
     */
    ReentrantLock getLock();

    /**
     * Gets coordinates or identifier of this node.
     */
    String getCoordinates();
}
