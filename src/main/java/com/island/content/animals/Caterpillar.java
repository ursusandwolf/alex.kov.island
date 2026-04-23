package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

// Тип животного: Caterpillar
public class Caterpillar extends Animal {

    public Caterpillar() {
        super(SpeciesConfig.getInstance().getAnimalType("caterpillar"));
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
    public Caterpillar reproduce() {
        if (!canPerformAction()) return null;
        return null; // TODO: реализация размножения
    }
}
