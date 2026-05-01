package com.island.nature.entities.plants;

import com.island.nature.entities.Biomass;
import com.island.nature.entities.SpeciesKey;

/**
 * Cabbage implementation.
 */
public class Cabbage extends Biomass {
    public Cabbage(long maxBiomass, int speed) {
        super("Cabbage", SpeciesKey.CABBAGE, maxBiomass, speed);
    }
}
