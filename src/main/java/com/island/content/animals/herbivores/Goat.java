package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Goat extends Animal implements Herbivore {
    public Goat() { super(SpeciesConfig.getInstance().getAnimalType("goat")); }
    @Override public String getTypeName() { return "Goat"; }
    @Override public String getSpeciesKey() { return "goat"; }
}
