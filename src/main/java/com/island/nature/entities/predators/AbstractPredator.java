package com.island.nature.entities.predators;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;

/**
 * Convenience abstract class for predators using integer arithmetic.
 */
public abstract class AbstractPredator extends Animal implements Predator {
    protected AbstractPredator(AnimalType type) {
        super(type);
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        return config.getScale10K();
    }

    @Override
    public int getPredatorMetabolismModifierBP() {
        return config.getScale10K();
    }
}
