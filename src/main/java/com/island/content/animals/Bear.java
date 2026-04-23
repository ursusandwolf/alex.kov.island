package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Bear extends Animal {
    public Bear() { super(SpeciesConfig.getInstance().getAnimalType("bear")); }
    @Override public String getTypeName() { return "Bear"; }
    @Override public String getSpeciesKey() { return "bear"; }
}
