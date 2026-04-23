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
        System.out.println("Животное " + getId().substring(0, 8) + " ищет еду...");
        return 0; // TODO: реализация поиска еды
    }

    @Override
    public Fox reproduce() {
        if (!canPerformAction()) return null;
        return null; // TODO: реализация размножения
    }
}
