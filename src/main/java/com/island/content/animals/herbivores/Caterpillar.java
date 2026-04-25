package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

// Тип животного: Caterpillar
public class Caterpillar extends Animal implements Herbivore {

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
        return 0; // TODO: реализация поиска еды
    }
}
