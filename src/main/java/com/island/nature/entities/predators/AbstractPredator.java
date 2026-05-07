package com.island.nature.entities.predators;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;


/**
 * Convenience abstract class for predators using integer arithmetic.
 */
public abstract class AbstractPredator extends Animal implements Predator {
    protected AbstractPredator(AnimalType type, ComponentRegistry registry) {
        super(type, registry);
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