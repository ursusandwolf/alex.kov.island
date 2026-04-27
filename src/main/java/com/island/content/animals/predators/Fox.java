package com.island.content.animals.predators;

import com.island.content.AnimalType;

public class Fox extends Predator {
    public Fox(AnimalType type) {
        super(type);
    }

    @Override
    public Fox reproduce() {
        return trySpendEnergyForReproduction() ? new Fox(animalType) : null;
    }
}
