package com.island.content.animals.predators;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Eagle extends Animal implements Predator {
    public Eagle() { super(SpeciesConfig.getInstance().getAnimalType("eagle")); }
    @Override public String getTypeName() { return "Eagle"; }
    @Override public String getSpeciesKey() { return "eagle"; }
}
