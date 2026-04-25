package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Wolf extends Animal implements Predator {
    public Wolf(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Wolf reproduce() {
        return trySpendEnergyForReproduction() ? new Wolf(animalType) : null;
    }
}
