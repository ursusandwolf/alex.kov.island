package com.island.nature.entities;

import com.island.nature.model.Cell;

/**
 * Interface for specialized biomass management operations.
 */
public interface BiomassManager {
    void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount);
}
