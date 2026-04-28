package com.island.content.plants;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Biomass {
    public Grass(double maxBiomass, int speed) {
        super("Grass", SpeciesKey.GRASS, maxBiomass, speed);
    }
}
