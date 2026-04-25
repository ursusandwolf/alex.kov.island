package com.island.content.plants;
import com.island.util.RandomUtils;


public class Cabbage extends Plant {
    public Cabbage() {
        super(100.0, 20.0, 0); // 100kg max, 20kg growth (was 50/5)
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
        // Higher reproduction chance to resist fast consumption
        if (canPerformAction() && RandomUtils.nextDouble() < 0.15) { // was 0.05
            return new Cabbage();
        }
        return null;
    }
}
