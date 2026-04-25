package com.island.content.plants;

import com.island.content.SpeciesConfig;

public class Cabbage extends Plant {
    public Cabbage() {
        super(SpeciesConfig.getInstance().getCabbageWeight() * SpeciesConfig.getInstance().getCabbageMaxCount()); 
    }

    @Override
    public String getTypeName() {
        return "Cabbage";
    }

    @Override
    public String getSpeciesKey() {
        return "cabbage";
    }
}
