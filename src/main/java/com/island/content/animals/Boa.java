package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Boa extends Animal {
    public Boa() { super(SpeciesConfig.getInstance().getAnimalType("boa")); }
    @Override public String getTypeName() { return "Boa"; }
    @Override public String getSpeciesKey() { return "boa"; }
}
