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
     * Gets the world this node belongs to.
     */
    SimulationWorld getWorld();

    /**
     * Gets all living entities in this node.
     */
    java.util.List<? extends Mortal> getLivingEntities();

    /**
     * Gets only biomass-based entities in this node.
     */
    java.util.List<? extends Mortal> getBiomassEntities();

    /**
     * Iterates over predators in this node.
     */
    void forEachPredator(java.util.function.Consumer<com.island.content.Animal> action);

    /**
     * Iterates over herbivores in this node with LOD sampling.
     */
    void forEachHerbivoreSampled(int limit, com.island.util.RandomProvider random, java.util.function.Consumer<com.island.content.Animal> action);

    /**
     * Iterates over all animals in this node with LOD sampling.
     */
    void forEachAnimalSampled(int limit, com.island.util.RandomProvider random, java.util.function.Consumer<com.island.content.Animal> action);

    /**
     * Gets the count of a specific species in this node.
     */
    int getOrganismCount(com.island.content.SpeciesKey key);

    /**
     * Removes all dead organisms from this node.
     * @return List of removed animals.
     */
    java.util.List<com.island.content.Animal> cleanupDeadOrganisms();

    /**
     * Adds an entity to this node.
     */
    boolean addEntity(Mortal entity);

    /**
     * Removes an entity from this node.
     */
    boolean removeEntity(Mortal entity);
}
