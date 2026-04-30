package com.island.content.plants;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;

/**
 * Cabbage implementation.
 */
public class Cabbage extends Biomass {
    public Cabbage(long maxBiomass, int speed) {
        super("Cabbage", SpeciesKey.CABBAGE, maxBiomass, speed);
    }
}
