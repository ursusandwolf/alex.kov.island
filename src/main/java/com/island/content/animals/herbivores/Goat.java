package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Goat extends Animal implements Herbivore {
    public Goat(AnimalType type) {
        super(type);
    }

    @Override
    public Goat reproduce() {
        return trySpendEnergyForReproduction() ? new Goat(animalType) : null;
    }
}
