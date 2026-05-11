package com.island.engine.model;

import com.island.engine.core.EngineAPI;

/**
 * Domain-agnostic snapshot of a simulation node state.
 */
@EngineAPI
public interface NodeSnapshot {
    /**
     * Gets the unique coordinates or identifier of the node.
     */
    String getCoordinates();

    /**
     * Gets the code of the species with the highest biomass or presence in this node.
     */
    String getTopSpeciesCode();

    /**
     * Checks if the top species in this node is a plant.
     */
    boolean isTopSpeciesPlant();

    /**
     * Checks if the node currently contains any organisms.
     */
    boolean hasOrganisms();
}