package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.engine.SimulationWorld;

/**
 * Domain-specific extension of SimulationWorld for nature/island simulation.
 * Aggregates specialized interfaces for modular usage.
 */
public interface NatureWorld extends SimulationWorld<Organism, Configuration>, 
        NatureRegistry, NatureStatistics, NatureEnvironment, BiomassManager {
    @Override
    Configuration getConfiguration();
}
