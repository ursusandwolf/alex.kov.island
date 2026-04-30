package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Convenience abstract class for predators using integer arithmetic.
 */
public abstract class AbstractPredator extends Animal implements Predator {
    protected AbstractPredator(AnimalType type) {
        super(type);
    }

    @Override
    protected int getSpecialMetabolismModifierBP() {
        return getPredatorMetabolismModifierBP();
    }
}
