package com.island.nature.entities.herbivores;

import com.island.nature.config.SimulationConstants;

/**
 * Interface for herbivores with integer-based metabolism and offspring logic.
 */
public interface Herbivore {
    default int getHerbivoreMetabolismModifierBP() {
        return SimulationConstants.HERBIVORE_METABOLISM_MODIFIER_BP;
    }

    default int getHerbivoreOffspringBonus() {
        return SimulationConstants.HERBIVORE_OFFSPRING_BONUS;
    }
}
