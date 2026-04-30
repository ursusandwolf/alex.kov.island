package com.island.content.animals.predators;

import static com.island.config.SimulationConstants.SCALE_10K;

/**
 * Interface for predators with integer-based metabolism modifiers.
 */
public interface Predator {
    default int getPredatorMetabolismModifierBP() {
        return SCALE_10K; // Default: 1.0 (10000 BP)
    }
}
