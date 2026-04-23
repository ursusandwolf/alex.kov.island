package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Buffalo extends Animal implements Herbivore {
    public Buffalo() { super(SpeciesConfig.getInstance().getAnimalType("buffalo")); }
    @Override public String getTypeName() { return "Buffalo"; }
    @Override public String getSpeciesKey() { return "buffalo"; }
}
