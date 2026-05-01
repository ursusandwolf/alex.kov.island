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
     * Iterates over all animals in this node without copying the underlying collection.
     * The implementation must ensure thread-safety (e.g., by holding a read lock).
     */
    void forEachAnimal(java.util.function.Consumer<com.island.content.Animal> action);

    /**
     * Gets only biomass-based entities in this node.
     */
    java.util.List<? extends Mortal> getBiomassEntities();

    /**
     * Iterates over all biomass entities in this node without copying.
     */
    void forEachBiomass(java.util.function.Consumer<com.island.content.Biomass> action);

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
     * Gets total animal count in this node.
     */
    int getAnimalCount();

    /**
     * Gets total biomass count in this node.
     */
    int getBiomassCount();

    /**
     * Gets the count of a specific species in this node.
     */
    int getOrganismCount(com.island.content.SpeciesKey key);

    /**
     * Checks if this node can accept the given animal (based on capacity and terrain).
     */
    boolean canAccept(com.island.content.Animal animal);

    /**
     * Adds an entity to this node.
     */
    boolean addEntity(Mortal entity);

    /**
     * Removes an entity from this node.
     */
    boolean removeEntity(Mortal entity);

    /**
     * Gets a random animal of a specific type from this node.
     */
    com.island.content.Animal getRandomAnimalByType(com.island.content.AnimalType type, com.island.util.RandomProvider random);

    /**
     * Cleans up dead entities in this node and calls the provided action for each removed animal.
     */
    void cleanupDeadEntities(java.util.function.Consumer<com.island.content.Animal> onAnimalRemoved);

    /**
     * Gets the biomass container for the specified species key.
     */
    com.island.content.Biomass getBiomass(com.island.content.SpeciesKey key);

    /**
     * Adds a biomass container to the node.
     */
    boolean addBiomass(com.island.content.Biomass b);
}
