package com.island.content.animals.herbivores;

import com.island.content.AnimalType;

public class Horse extends Herbivore {
    public Horse(AnimalType type) {
        super(type);
    }

    @Override
    public Horse reproduce() {
        return trySpendEnergyForReproduction() ? new Horse(animalType) : null;
    }
}
