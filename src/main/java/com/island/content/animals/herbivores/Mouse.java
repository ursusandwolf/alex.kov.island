package com.island.content.animals.herbivores;

import com.island.content.AnimalType;

public class Mouse extends Herbivore {
    public Mouse(AnimalType type) {
        super(type);
    }

    @Override
    public Mouse reproduce() {
        return trySpendEnergyForReproduction() ? new Mouse(animalType) : null;
    }
}
