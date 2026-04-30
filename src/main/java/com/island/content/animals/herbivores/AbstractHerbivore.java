package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

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
