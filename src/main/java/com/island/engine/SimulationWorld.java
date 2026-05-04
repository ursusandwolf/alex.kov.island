package com.island.engine;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Represents the spatial environment of the simulation.
 * Generic interface decoupled from specific domains (like nature/animals).
 *
 * @param <T> The base type of entities in this world.
 */
public interface SimulationWorld<T extends Mortal, C> extends Tickable {
    /**
     * Gets the configuration object for this world.
     */
    C getConfiguration();

    /**
     * Returns "work units" (e.g. chunks) of nodes for parallel processing.
     */
    Collection<? extends Collection<? extends SimulationNode<T>>> getParallelWorkUnits();

    /**
     * Gets a specific node by relative coordinates from a current node.
     * @return Empty if coordinates are out of bounds.
     */
    Optional<SimulationNode<T>> getNode(SimulationNode<T> current, int dx, int dy);

    /**
     * Moves an entity between nodes.
     * @return true if movement was successful.
     */
    boolean moveEntity(T entity, SimulationNode<T> from, SimulationNode<T> to);

    /**
     * Gets world width.
     */
    int getWidth();

    /**
     * Gets world height.
     */
    int getHeight();

    /**
     * Creates a domain-agnostic snapshot of the current world state.
     */
    WorldSnapshot createSnapshot();

    /**
     * Performs initialization of the world (e.g., topology setup).
     */
    default void initialize() { }

    /**
     * Adds a listener to the world.
     */
    void addListener(WorldListener<T> listener);

    /**
     * Gets all registered listeners.
     */
    List<WorldListener<T>> getListeners();

    /**
     * Gets the event bus associated with this world.
     */
    com.island.engine.event.EventBus getEventBus();

    /**
     * Sets the event bus for this world.
     */
    void setEventBus(com.island.engine.event.EventBus eventBus);
}
