package com.island.content.animals.predators;

import com.island.content.AnimalType;

public class Wolf extends Predator {
    public Wolf(AnimalType type) {
        super(type);
    }

    @Override
    public Wolf reproduce() {
        return trySpendEnergyForReproduction() ? new Wolf(animalType) : null;
    }
}
