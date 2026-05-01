package com.island.nature.entities.plants;

import com.island.nature.entities.Biomass;
import com.island.nature.entities.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Biomass {
    public Grass(long maxBiomass, int speed) {
        super("Grass", SpeciesKey.GRASS, maxBiomass, speed);
    }
}
