package com.island.content.animals.predators;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Boa extends Animal implements Predator {
    public Boa() { super(SpeciesConfig.getInstance().getAnimalType("boa")); }
    @Override public String getTypeName() { return "Boa"; }
    @Override public String getSpeciesKey() { return "boa"; }
}
