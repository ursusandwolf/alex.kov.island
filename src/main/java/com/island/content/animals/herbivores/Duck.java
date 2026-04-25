package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Duck extends Animal implements Herbivore {
    public Duck(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Duck reproduce() {
        return trySpendEnergyForReproduction() ? new Duck(animalType) : null;
    }
}
