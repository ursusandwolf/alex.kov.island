package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Goat extends Animal {
    public Goat() { super(SpeciesConfig.getInstance().getAnimalType("goat")); }
    @Override public String getTypeName() { return "Goat"; }
    @Override public String getSpeciesKey() { return "goat"; }
}
