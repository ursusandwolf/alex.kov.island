package com.island.content.animals.herbivores;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Convenience abstract class for herbivores.
 * Avoids the need for instanceof checks by overriding methods directly.
 */
public abstract class AbstractHerbivore extends Animal implements Herbivore {
    protected AbstractHerbivore(AnimalType type) {
        super(type);
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        return getHerbivoreMetabolismModifier();
    }

    @Override
    public int getOffspringBonus() {
        return getHerbivoreOffspringBonus();
    }
}
