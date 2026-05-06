package com.island.nature.entities.domain;

import com.island.nature.config.Configuration;
import com.island.engine.core.SimulationWorld;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.entities.registry.NatureRegistry;

/**
 * Domain-specific extension of SimulationWorld for nature/island simulation.
 * Aggregates specialized interfaces for modular usage.
 */
public interface NatureWorld extends SimulationWorld<Organism>, 
        NatureRegistry, NatureStatistics, NatureEnvironment, BiomassManager {
    Configuration getConfiguration();
}