package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

public class Boa extends Animal implements Predator {
    public Boa(AnimalType type) {
        super(type);
    }

    @Override
    public Boa reproduce() {
        return trySpendEnergyForReproduction() ? new Boa(animalType) : null;
    }
}
