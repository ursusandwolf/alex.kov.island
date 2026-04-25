package com.island.content.plants;

import com.island.content.SpeciesConfig;

public class Grass extends Plant {
    public Grass() {
        super(SpeciesConfig.getInstance().getPlantWeight() * SpeciesConfig.getInstance().getPlantMaxCount()); 
    }

    @Override
    public String getTypeName() {
        return "Grass";
    }

    @Override
    public String getSpeciesKey() {
        return "plant";
    }
}
