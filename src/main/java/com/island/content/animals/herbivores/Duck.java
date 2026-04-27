package com.island.content.animals.herbivores;

import com.island.content.AnimalType;

public class Duck extends Herbivore {
    public Duck(AnimalType type) {
        super(type);
    }

    @Override
    public Duck reproduce() {
        return trySpendEnergyForReproduction() ? new Duck(animalType) : null;
    }
}
