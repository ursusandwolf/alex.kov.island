package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Eagle extends Animal implements Predator {
    public Eagle(AnimalType type) {
        super(type);
    }

    @Override
    public Eagle reproduce() {
        return trySpendEnergyForReproduction() ? new Eagle(animalType) : null;
    }
}
