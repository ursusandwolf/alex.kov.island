package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Rabbit extends Animal implements Herbivore {
    public Rabbit(AnimalType type) {
        super(type);
    }

    @Override
    public Rabbit reproduce() {
        return trySpendEnergyForReproduction() ? new Rabbit(animalType) : null;
    }
}
