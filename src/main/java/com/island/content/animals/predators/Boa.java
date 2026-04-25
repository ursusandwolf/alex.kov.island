package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Boa extends Animal implements Predator {
    public Boa(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Boa reproduce() {
        return trySpendEnergyForReproduction() ? new Boa(animalType) : null;
    }
}
