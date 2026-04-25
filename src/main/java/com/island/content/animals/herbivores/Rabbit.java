package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

// Тип животного: Rabbit
public class Rabbit extends Animal implements Herbivore {

    public Rabbit() {
        super(SpeciesConfig.getInstance().getAnimalType("rabbit"));
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
