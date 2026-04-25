package com.island.content.plants;
import com.island.util.RandomUtils;


public class Grass extends Plant {
    public Grass() {
        super(200.0, 10.0, 0); // 200kg max, 10kg growth per tick
    }

    @Override
    public String getTypeName() {
        return "Grass";
    }

    @Override
    public String getSpeciesKey() {
        return "plant"; // Map to "plant" in interaction matrix
    }

    @Override
    public void checkState() {
        super.checkState();
        grow(); // Plants grow every tick
    }

    @Override
    public Grass reproduce() {
        // Grass spreads with a small chance
        if (canPerformAction() && RandomUtils.nextDouble() < 0.1) {
            return new Grass();
        }
        return null;
    }
}
