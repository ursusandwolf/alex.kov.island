package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Sheep extends Animal implements Herbivore {
    public Sheep() { super(SpeciesConfig.getInstance().getAnimalType("sheep")); }
    @Override public String getTypeName() { return "Sheep"; }
    @Override public String getSpeciesKey() { return "sheep"; }
}
