package com.island.content.animals.herbivores;

import com.island.config.SimulationConstants;

/**
 * Interface for herbivores.
 * Provides default values for herbivore-specific behavior.
 */
public interface Herbivore {
    default double getHerbivoreMetabolismModifier() {
        return SimulationConstants.HERBIVORE_METABOLISM_MODIFIER;
    }

    default int getHerbivoreOffspringBonus() {
        return SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
    }
}
