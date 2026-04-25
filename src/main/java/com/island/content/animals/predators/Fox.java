package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Fox extends Animal implements Predator {
    public Fox(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Fox reproduce() {
        return trySpendEnergyForReproduction() ? new Fox(animalType) : null;
    }
}
