package com.island.content.plants;


import com.island.content.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Plant {
    public Grass(double maxBiomass) {
        super("Grass", SpeciesKey.GRASS, maxBiomass);
    }
}

