package com.island.util;

import com.island.content.SpeciesKey;

/**
 * Strategy/Provider for predator-prey interaction probabilities.
 */
public interface InteractionProvider {
    /**
     * Gets the success chance for a predator-prey pair (0-100).
     */
    int getChance(SpeciesKey predator, SpeciesKey prey);

    /**
     * Checks if the predator has any animal prey in this matrix.
     */
    boolean hasAnimalPrey(SpeciesKey predator);
}
