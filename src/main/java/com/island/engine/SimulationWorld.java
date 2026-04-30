package com.island.engine;

import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.service.StatisticsService;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Represents the spatial environment of the simulation.
 * Updated to use integer-based protection chances and biomass amounts.
 */
public interface SimulationWorld extends Tickable {
    /**
     * Returns "work units" (chunks) of nodes for parallel processing.
     */
    Collection<? extends Collection<? extends SimulationNode>> getParallelWorkUnits();

    /**
     * Gets a specific node by relative coordinates.
     * @return Empty if coordinates are out of bounds.
     */
    Optional<SimulationNode> getNode(SimulationNode current, int dx, int dy);

    /**
     * Moves an animal between nodes.
     */
    void moveAnimal(Animal animal, SimulationNode from, SimulationNode to);

    /**
     * Moves biomass between nodes.
     */
    void moveBiomassPartially(Biomass b, SimulationNode from, SimulationNode to, long amount);

    /**
     * Reports death of a species to the world statistics.
     */
    void reportDeath(SpeciesKey key, DeathCause cause);

    /**
     * Called when a new organism is added to any node.
     */
    void onOrganismAdded(SpeciesKey key);

    /**
     * Called when an organism is removed from any node.
     */
    void onOrganismRemoved(SpeciesKey key);

    /**
     * Gets world width.
     */
    int getWidth();

    /**
     * Gets world height.
     */
    int getHeight();

    /**
     * Gets the total count of a species on the island.
     */
    int getSpeciesCount(SpeciesKey key);

    /**
     * Gets a map of species-specific protection chances (0-100 percent).
     */
    Map<SpeciesKey, Integer> getProtectionMap(SpeciesRegistry registry);

    /**
     * Gets the statistics service for the world.
     */
    StatisticsService getStatisticsService();

    /**
     * Gets the species registry for the world.
     */
    SpeciesRegistry getRegistry();

    /**
     * Gets the current season of the world.
     */
    default Season getCurrentSeason() {
        return Season.SPRING;
    }

    /**
     * Creates a domain-agnostic snapshot of the current world state.
     */
    WorldSnapshot createSnapshot();
}
