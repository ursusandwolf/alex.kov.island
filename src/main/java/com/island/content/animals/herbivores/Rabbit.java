package com.island.content.animals.herbivores;

import com.island.content.AnimalType;

public class Rabbit extends Herbivore {
    public Rabbit(AnimalType type) {
        super(type);
    }

    @Override
    public Rabbit reproduce() {
        return trySpendEnergyForReproduction() ? new Rabbit(animalType) : null;
    }
}
