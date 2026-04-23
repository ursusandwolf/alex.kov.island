package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Deer extends Animal implements Herbivore {
    public Deer() { super(SpeciesConfig.getInstance().getAnimalType("deer")); }
    @Override public String getTypeName() { return "Deer"; }
    @Override public String getSpeciesKey() { return "deer"; }
}
