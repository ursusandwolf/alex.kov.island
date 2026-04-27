package com.island.content.plants;

import com.island.content.SpeciesConfig;
import com.island.content.SpeciesKey;

/**
 * Grass implementation.
 */
public class Grass extends Plant {
    public Grass() {
        super("Grass", SpeciesKey.GRASS, 
                SpeciesConfig.getInstance().getPlantWeight() * SpeciesConfig.getInstance().getPlantMaxCount()); 
    }
}
