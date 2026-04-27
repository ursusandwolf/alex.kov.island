package com.island.content.plants;

import com.island.content.SpeciesKey;
import com.island.content.SpeciesConfig;

public class Grass extends Plant {
    public Grass() {
        super("Grass", SpeciesKey.GRASS, 
              SpeciesConfig.getInstance().getPlantWeight() * SpeciesConfig.getInstance().getPlantMaxCount()); 
    }
}
