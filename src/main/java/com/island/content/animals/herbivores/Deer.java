package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Deer extends Animal implements Herbivore {
    public Deer(AnimalType type) {
        super(type);
    }

    @Override
    public Deer reproduce() {
        return trySpendEnergyForReproduction() ? new Deer(animalType) : null;
    }
}
