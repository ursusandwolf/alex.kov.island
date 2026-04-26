package com.island.content.plants;

import com.island.content.SpeciesConfig;

public class Cabbage extends Plant {
    public Cabbage() {
        super("Cabbage", "cabbage", 
              SpeciesConfig.getInstance().getCabbageWeight() * SpeciesConfig.getInstance().getCabbageMaxCount()); 
    }
}
