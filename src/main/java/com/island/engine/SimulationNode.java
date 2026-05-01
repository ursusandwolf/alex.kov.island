package com.island.engine;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.util.RandomProvider;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;

/**
 * Represents a spatial unit in the simulation where entities reside.
 */
public interface SimulationNode {
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
    void setNeighbors(List<SimulationNode> neighbors);

    /**
     * Gets pre-calculated neighbors.
     */
    List<SimulationNode> getNeighbors();

    /**
     * Gets the world this node belongs to.
     */
    SimulationWorld getWorld();

    /**
     * Gets all living entities in this node.
     */
    List<? extends Mortal> getLivingEntities();

    /**
     * Iterates over all animals in this node without copying the underlying collection.
     * The implementation must ensure thread-safety (e.g., by holding a read lock).
     */
    void forEachAnimal(Consumer<Animal> action);

    /**
     * Gets only biomass-based entities in this node.
     */
    List<? extends Mortal> getBiomassEntities();

    /**
     * Iterates over all biomass entities in this node without copying.
     */
    void forEachBiomass(Consumer<Biomass> action);

    /**
     * Iterates over predators in this node.
     */
    void forEachPredator(Consumer<Animal> action);

    /**
     * Iterates over herbivores in this node with LOD sampling.
     */
    void forEachHerbivoreSampled(int limit, RandomProvider random, Consumer<Animal> action);

    /**
     * Iterates over all animals in this node with LOD sampling.
     */
    void forEachAnimalSampled(int limit, RandomProvider random, Consumer<Animal> action);

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
    int getOrganismCount(SpeciesKey key);

    /**
     * Checks if this node can accept the given animal (based on capacity and terrain).
     */
    boolean canAccept(Animal animal);

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
    Animal getRandomAnimalByType(AnimalType type, RandomProvider random);

    /**
     * Cleans up dead entities in this node and calls the provided action for each removed animal.
     */
    void cleanupDeadEntities(Consumer<Animal> onAnimalRemoved);

    /**
     * Gets the biomass container for the specified species key.
     */
    Biomass getBiomass(SpeciesKey key);

    /**
     * Adds a biomass container to the node.
     */
    boolean addBiomass(Biomass b);
}
