package com.island.nature.entities.plants;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.SpeciesKey;

/**
 * Mushroom implementation.
 */
public class Mushroom extends Biomass {
    public Mushroom(Configuration config, long maxBiomass, int speed) {
        super(config, "Mushroom", SpeciesKey.MUSHROOM, maxBiomass, speed);
    }
}
