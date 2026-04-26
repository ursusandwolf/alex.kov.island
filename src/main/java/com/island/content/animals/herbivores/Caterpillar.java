package com.island.content.animals.herbivores;

import com.island.content.plants.Plant;
import com.island.content.SpeciesConfig;

/**
 * Optimized Caterpillar: Now acts as a biomass container (like plants).
 * This eliminates millions of individual objects from the simulation.
 */
public class Caterpillar extends Plant {
    public Caterpillar() {
        super(SpeciesConfig.getInstance().getAnimalType("caterpillar").getWeight() * 
              SpeciesConfig.getInstance().getAnimalType("caterpillar").getMaxPerCell());
    }

    @Override
    public String getTypeName() {
        return "Caterpillar";
    }

    @Override
    public String getSpeciesKey() {
        return "caterpillar";
    }
}
