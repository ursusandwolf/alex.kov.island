package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;
import static com.island.config.SimulationConstants.HERBIVORE_METABOLISM_MODIFIER;
import static com.island.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;

/**
 * Interface/Base for herbivores.
 */
public abstract class Herbivore extends Animal {
    public Herbivore(AnimalType type) {
        super(type);
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        return HERBIVORE_METABOLISM_MODIFIER;
    }

    @Override
    public int getOffspringBonus() {
        return HERBIVORE_OFFSPRING_BONUS;
    }
}
