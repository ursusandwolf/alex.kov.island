package com.island.nature.entities.registry;

import com.island.nature.model.Cell;
import com.island.nature.entities.core.Biomass;

/**
 * Interface for specialized biomass management operations.
 */
public interface BiomassManager {
    void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount);
}