package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Caterpillar extends Animal implements Herbivore {
    public Caterpillar(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Caterpillar reproduce() {
        return trySpendEnergyForReproduction() ? new Caterpillar(animalType) : null;
    }
}
