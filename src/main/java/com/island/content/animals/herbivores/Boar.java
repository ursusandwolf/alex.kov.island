package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Boar extends Animal implements Herbivore {
    public Boar(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Boar reproduce() {
        return trySpendEnergyForReproduction() ? new Boar(animalType) : null;
    }
}
