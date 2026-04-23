package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Horse extends Animal implements Herbivore {
    public Horse() { super(SpeciesConfig.getInstance().getAnimalType("horse")); }
    @Override public String getTypeName() { return "Horse"; }
    @Override public String getSpeciesKey() { return "horse"; }
}
