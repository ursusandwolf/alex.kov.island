package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

// Хищник: Волк
public class Wolf extends Animal implements Predator {

    public Wolf() {
        super(SpeciesConfig.getInstance().getAnimalType("wolf"));
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public double eat() {
        if (!isAlive()) return 0;
        return 0; // TODO: реализация охоты
    }
}
