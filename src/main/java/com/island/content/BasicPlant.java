package com.island.content;

import java.util.concurrent.ThreadLocalRandom;

public class BasicPlant extends Plant {
    public BasicPlant() {
        super(1.0, 1.0, 0); // 1kg max, 1kg growth per tick, immortal
    }

    @Override
    public String getTypeName() {
        return "Plant";
    }

    @Override
    public String getSpeciesKey() {
        return "plant";
    }

    @Override
    public BasicPlant reproduce() {
        // Plants spread with 10% chance if they have enough biomass (energy)
        if (canPerformAction() && ThreadLocalRandom.current().nextDouble() < 0.1) {
            return new BasicPlant();
        }
        return null;
    }
}
