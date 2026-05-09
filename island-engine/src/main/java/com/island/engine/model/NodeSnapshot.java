package com.island.engine.model;

import com.island.engine.core.EngineAPI;

/**
 * Domain-agnostic snapshot of a simulation node state.
 */
@EngineAPI
public interface NodeSnapshot {
    String getCoordinates();

    /**
     * Code of the species with the highest biomass/presence in this node.
     */
    String getTopSpeciesCode();

    /**
     * Whether the top species is a plant.
     */
    boolean isTopSpeciesPlant();

    /**
     * Whether the node has any organisms.
     */
    boolean hasOrganisms();
}