package com.island.content.animals.herbivores;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Mouse extends Animal implements Herbivore {
    public Mouse() { super(SpeciesConfig.getInstance().getAnimalType("mouse")); }
    @Override public String getTypeName() { return "Mouse"; }
    @Override public String getSpeciesKey() { return "mouse"; }
}
