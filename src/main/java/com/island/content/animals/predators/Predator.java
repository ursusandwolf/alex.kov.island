package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;

/**
 * Interface/Base for predators.
 */
public abstract class Predator extends Animal {
    public Predator(AnimalType type) {
        super(type);
    }
}
