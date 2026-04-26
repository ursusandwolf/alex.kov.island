package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Sheep extends Animal implements Herbivore {
    public Sheep(AnimalType type) {
        super(type);
    }

    @Override
    public Sheep reproduce() {
        return trySpendEnergyForReproduction() ? new Sheep(animalType) : null;
    }
}
