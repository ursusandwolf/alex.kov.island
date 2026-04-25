package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Mouse extends Animal implements Herbivore {
    public Mouse(AnimalType type) {
        super(type);
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public Mouse reproduce() {
        return trySpendEnergyForReproduction() ? new Mouse(animalType) : null;
    }
}
