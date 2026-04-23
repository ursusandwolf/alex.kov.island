package com.island.content.animals;
import com.island.content.Animal;
import com.island.content.SpeciesConfig;
public class Mouse extends Animal {
    public Mouse() { super(SpeciesConfig.getInstance().getAnimalType("mouse")); }
    @Override public String getTypeName() { return "Mouse"; }
    @Override public String getSpeciesKey() { return "mouse"; }
}
