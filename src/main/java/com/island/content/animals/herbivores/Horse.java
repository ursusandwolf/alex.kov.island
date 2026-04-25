package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Horse extends Animal implements Herbivore {
    public Horse(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Horse reproduce() {
        return trySpendEnergyForReproduction() ? new Horse(animalType) : null;
    }
}
