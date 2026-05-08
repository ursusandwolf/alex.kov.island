package com.island.nature.entities.plants;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Biomass {
    public Grass(Configuration config, ComponentRegistry registry, SpeciesKey key, long maxBiomass, int speed) {
        super(config, registry, "Grass", key, maxBiomass, speed);
    }
}