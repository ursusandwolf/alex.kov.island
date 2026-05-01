package com.island.engine;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * Represents a spatial unit in the simulation where entities reside.
 * Generic interface that is decoupled from specific entity types like Animals or Plants.
 *
 * @param <T> The base type of entities that can inhabit this node.
 */
public interface SimulationNode<T extends Mortal> {
    /**
     * Gets the synchronization lock for this node (usually the write lock).
     */
    Lock getLock();

    /**
     * Gets coordinates or identifier of this node.
     */
    String getCoordinates();

    /**
     * Caches neighbors for fast access.
     */
    void setNeighbors(List<SimulationNode<T>> neighbors);

    /**
     * Gets pre-calculated neighbors.
     */
    List<SimulationNode<T>> getNeighbors();

    /**
     * Gets the world this node belongs to.
     */
    SimulationWorld<T> getWorld();

    /**
     * Gets all living entities in this node.
     */
    List<T> getEntities();

    /**
     * Iterates over all entities in this node.
     * The implementation must ensure thread-safety (e.g., by holding a lock).
     */
    void forEachEntity(Consumer<T> action);

    /**
     * Gets total entity count in this node.
     */
    int getEntityCount();

    /**
     * Checks if this node can accept the given entity.
     */
    boolean canAccept(T entity);

    /**
     * Adds an entity to this node.
     */
    boolean addEntity(T entity);

    /**
     * Removes an entity from this node.
     */
    boolean removeEntity(T entity);

    /**
     * Cleans up dead entities in this node and calls the provided action for each removed entity.
     */
    void cleanupDeadEntities(Consumer<T> onEntityRemoved);
}
