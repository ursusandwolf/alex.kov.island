package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Wolf extends Animal implements Predator {
    public Wolf(AnimalType type) {
        super(type);
    }

    public Wolf reproduce() {
        return trySpendEnergyForReproduction() ? new Wolf(animalType) : null;
    }
}
