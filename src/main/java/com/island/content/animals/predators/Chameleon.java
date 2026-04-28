package com.island.content.animals.predators;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.util.RandomUtils;

/**
 * Chameleon has a unique protection mechanic: 80% invisibility.
 */
public class Chameleon extends Animal {
    public Chameleon(AnimalType type) {
        super(type);
    }

    @Override
    public boolean isProtected(int currentTick) {
        // Unique ability: 80% chance to be invisible to predators
        return super.isProtected(currentTick) || RandomUtils.nextDouble() < 0.80;
    }
}
