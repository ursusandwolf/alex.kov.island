package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Eagle extends Animal implements Predator {
    public Eagle(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Eagle reproduce() {
        return trySpendEnergyForReproduction() ? new Eagle(animalType) : null;
    }
}
