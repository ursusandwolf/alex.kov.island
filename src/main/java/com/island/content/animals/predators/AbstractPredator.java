package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Convenience abstract class for predators.
 * Avoids the need for instanceof checks by overriding methods directly.
 */
public abstract class AbstractPredator extends Animal implements Predator {
    protected AbstractPredator(AnimalType type) {
        super(type);
    }

    @Override
    protected double getSpecialMetabolismModifier() {
        return getPredatorMetabolismModifier();
    }
}
