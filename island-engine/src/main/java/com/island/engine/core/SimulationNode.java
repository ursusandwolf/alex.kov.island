package com.island.engine.core;

import com.island.engine.ecs.EntityQuery;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import com.island.engine.model.Mortal;

/**
 * Represents a spatial unit in the simulation where entities reside.
 * 
 * <p>A node is a container for entities and provides methods for entity management 
 * (adding, removing, querying). In a grid-based simulation, each node corresponds 
 * to a single cell.</p>
 * 
 * <p>Thread Safety: All operations on a node must be protected by its internal lock, 
 * which can be retrieved via {@link #getLock()}.</p>
 *
 * @param <T> The base type of entities that can inhabit this node.
 * @since 1.0
 */
@EngineAPI
public interface SimulationNode<T extends Mortal> {
    /**
     * Gets the synchronization lock for this node. 
     * Callers must acquire this lock before performing any entity operations.
     * 
     * @return the node's internal lock.
     */
    Lock getLock();

    /**
     * Returns a human-readable identifier for this node (e.g., "x:5, y:10").
     */
    String getCoordinates();

    /**
     * Assigns neighbors to this node. Usually called once during world initialization.
     * 
     * @param neighbors the list of adjacent nodes.
     */
    void setNeighbors(List<SimulationNode<T>> neighbors);

    /**
     * Retrieves the pre-calculated list of neighbors.
     */
    List<SimulationNode<T>> getNeighbors();

    /**
     * Returns the {@link SimulationWorld} instance that owns this node.
     */
    SimulationWorld<T> getWorld();

    /**
     * Returns an unmodifiable list of all living entities currently in this node.
     * 
     * @return a list of entities.
     */
    List<T> getEntities();

    /**
     * Executes the given action for every entity currently in this node.
     * 
     * <p>Implementation must ensure that the iteration is thread-safe, 
     * usually by iterating over a snapshot or holding the lock.</p>
     * 
     * @param action the callback to execute for each entity.
     */
    void forEachEntity(Consumer<T> action);

    /**
     * Executes the given action for each entity that matches the specified query.
     * 
     * @param query  the filter criteria.
     * @param action the callback to execute for each matching entity.
     */
    default void query(EntityQuery<T> query, Consumer<T> action) {
        forEachEntity(entity -> {
            if (query.matches(entity)) {
                action.accept(entity);
            }
        });
    }

    /**
     * Returns the current number of entities residing in this node.
     */
    int getEntityCount();

    /**
     * Evaluates whether the given entity can be added to this node.
     * (e.g., checks for capacity limits or terrain compatibility).
     * 
     * @param entity the entity to check.
     * @return {@code true} if the node can accept the entity.
     */
    boolean canAccept(T entity);

    /**
     * Adds an entity to this node. 
     * Must be called while holding the lock.
     * 
     * @param entity the entity to add.
     * @return {@code true} if the entity was successfully added.
     */
    boolean addEntity(T entity);

    /**
     * Removes an entity from this node. 
     * Must be called while holding the lock.
     * 
     * @param entity the entity to remove.
     * @return {@code true} if the entity was successfully removed.
     */
    boolean removeEntity(T entity);

    /**
     * Scans for and removes entities that are no longer alive.
     * 
     * @param onEntityRemoved callback invoked for every removed dead entity.
     */
    void cleanupDeadEntities(Consumer<T> onEntityRemoved);

    /**
     * Hook called after an entity is added to the node.
     */
    default void onEntityAdded(T entity) { }

    /**
     * Hook called after an entity is removed from the node.
     */
    default void onEntityRemoved(T entity) { }
}