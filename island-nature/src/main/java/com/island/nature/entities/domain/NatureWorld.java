package com.island.nature.entities.domain;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.engine.core.SimulationWorld;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.entities.registry.NatureRegistry;

import com.island.nature.entities.core.Animal;
import com.island.nature.model.Cell;
import java.util.Optional;

/**
 * Domain-specific extension of SimulationWorld for nature/island simulation.
 * Aggregates specialized interfaces for modular usage.
 */
public interface NatureWorld extends SimulationWorld<Organism>, 
        NatureRegistry, NatureStatistics, NatureEnvironment, BiomassManager {
    Configuration getConfiguration();

    ComponentRegistry getComponentRegistry();

    /**
     * Gets a specific cell by relative coordinates from a current cell.
     * Nature-specific version to avoid SimulationNode narrowing.
     */
    Optional<Cell> getCell(Cell current, int dx, int dy);

    /**
     * Type-safe movement for animals within the nature world.
     */
    boolean moveOrganism(Animal animal, Cell from, Cell to);
}