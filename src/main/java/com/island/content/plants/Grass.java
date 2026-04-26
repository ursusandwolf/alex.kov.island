package com.island.content.plants;

import com.island.content.SpeciesConfig;

public class Grass extends Plant {
    public Grass() {
        super("Grass", "plant", 
              SpeciesConfig.getInstance().getPlantWeight() * SpeciesConfig.getInstance().getPlantMaxCount()); 
    }
}
