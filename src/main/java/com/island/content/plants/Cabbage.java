package com.island.content.plants;

import java.util.concurrent.ThreadLocalRandom;

public class Cabbage extends Plant {
    public Cabbage() {
        super(50.0, 5.0, 0); // 50kg max per unit, 5kg growth
    }

    @Override
    public String getTypeName() {
        return "Cabbage";
    }

    @Override
    public String getSpeciesKey() {
        return "cabbage"; // New key for specific herbivores
    }

    @Override
    public void checkState() {
        super.checkState();
        grow();
    }

    @Override
    public Cabbage reproduce() {
        // Cabbage spreads with a smaller chance
        if (canPerformAction() && ThreadLocalRandom.current().nextDouble() < 0.05) {
            return new Cabbage();
        }
        return null;
    }
}
