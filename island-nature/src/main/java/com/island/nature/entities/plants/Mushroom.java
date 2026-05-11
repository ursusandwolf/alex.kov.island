package com.island.nature.entities.plants;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.SpeciesKey;

/**
 * Mushroom implementation.
 */
public class Mushroom extends Biomass {
    public Mushroom(Configuration config, ComponentRegistry registry, SpeciesKey key, long maxBiomass, int speed) {
        super(config, registry, "Mushroom", key, maxBiomass, speed);
    }
}