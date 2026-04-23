package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Buffalo extends Animal {
    public Buffalo() { super(SpeciesConfig.getInstance().getAnimalType("buffalo")); }
    @Override public String getTypeName() { return "Buffalo"; }
    @Override public String getSpeciesKey() { return "buffalo"; }
}
