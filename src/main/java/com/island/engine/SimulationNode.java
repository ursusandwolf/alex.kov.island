package com.island.engine;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a spatial unit in the simulation where entities reside.
 */
public interface SimulationNode {
    /**
     * Gets the synchronization lock for this node (usually the write lock).
     */
    java.util.concurrent.locks.Lock getLock();

    /**
     * Gets coordinates or identifier of this node.
     */
    String getCoordinates();

    /**
     * Caches neighbors for fast access.
     */
    void setNeighbors(java.util.List<SimulationNode> neighbors);

    /**
     * Gets pre-calculated neighbors.
     */
    java.util.List<SimulationNode> getNeighbors();

    /**
     * Gets all living entities in this node.
     */
    java.util.List<? extends Mortal> getLivingEntities();

    /**
     * Gets only biomass-based entities in this node.
     */
    java.util.List<? extends Mortal> getBiomassEntities();

    /**
     * Adds an entity to this node.
     */
    boolean addEntity(Mortal entity);

    /**
     * Removes an entity from this node.
     */
    boolean removeEntity(Mortal entity);
}
