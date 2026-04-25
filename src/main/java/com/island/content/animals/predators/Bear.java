package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Bear extends Animal implements Predator {
    public Bear(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Bear reproduce() {
        return trySpendEnergyForReproduction() ? new Bear(animalType) : null;
    }
}
