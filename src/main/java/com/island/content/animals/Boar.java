package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Boar extends Animal {
    public Boar() { super(SpeciesConfig.getInstance().getAnimalType("boar")); }
    @Override public String getTypeName() { return "Boar"; }
    @Override public String getSpeciesKey() { return "boar"; }
}
