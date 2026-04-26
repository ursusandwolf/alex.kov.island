package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Buffalo extends Animal implements Herbivore {
    public Buffalo(AnimalType type) {
        super(type);
    }

    @Override
    public Buffalo reproduce() {
        return trySpendEnergyForReproduction() ? new Buffalo(animalType) : null;
    }
}
