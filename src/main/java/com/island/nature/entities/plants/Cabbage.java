package com.island.nature.entities.plants;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.SpeciesKey;

/**
 * Cabbage implementation.
 */
public class Cabbage extends Biomass {
    public Cabbage(Configuration config, long maxBiomass, int speed) {
        super(config, "Cabbage", SpeciesKey.CABBAGE, maxBiomass, speed);
    }
}
