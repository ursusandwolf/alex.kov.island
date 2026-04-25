package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

// Тип животного: Fox
public class Fox extends Animal implements Predator {

    public Fox() {
        super(SpeciesConfig.getInstance().getAnimalType("fox"));
    }

    @Override
    public String getTypeName() { return animalType.getTypeName(); }

    @Override
    public String getSpeciesKey() { return animalType.getSpeciesKey(); }

    @Override
    public double eat() {
        if (!isAlive()) return 0;
        return 0; // TODO: реализация поиска еды
    }
}
