package com.island.content.animals.predators;

/**
 * Interface for predators.
 * Provides default values for predator-specific behavior.
 */
public interface Predator {
    default double getPredatorMetabolismModifier() {
        return 1.0; // Default: no modifier
    }
}
