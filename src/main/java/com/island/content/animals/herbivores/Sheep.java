package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Sheep extends Animal implements Herbivore {
    public Sheep(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Sheep reproduce() {
        return trySpendEnergyForReproduction() ? new Sheep(animalType) : null;
    }
}
