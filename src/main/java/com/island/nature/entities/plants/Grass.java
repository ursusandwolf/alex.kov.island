package com.island.nature.entities.plants;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Biomass {
    public Grass(Configuration config, long maxBiomass, int speed) {
        super(config, "Grass", SpeciesKey.GRASS, maxBiomass, speed);
    }
}
