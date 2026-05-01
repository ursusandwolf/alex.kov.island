package com.island.nature.entities.herbivores;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;

/**
 * Convenience abstract class for herbivores using integer arithmetic.
 */
public abstract class AbstractHerbivore extends Animal implements Herbivore {
    protected AbstractHerbivore(AnimalType type) {
        super(type);
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        return getHerbivoreMetabolismModifierBP();
    }

    @Override
    public int getOffspringBonus() {
        return getHerbivoreOffspringBonus();
    }
}
