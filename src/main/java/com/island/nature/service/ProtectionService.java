package com.island.nature.service;

import com.island.nature.entities.SpeciesKey;
import java.util.Map;

/**
 * Service responsible for identifying and protecting endangered species.
 */
public interface ProtectionService {
    /**
     * Calculates the protection modifiers (e.g., hide chance) for endangered species.
     * @return Map of species key to hide chance in basis points (0-10000).
     */
    Map<SpeciesKey, Integer> getProtectionModifiers();
    
    /**
     * Updates the protection state for the current tick.
     */
    void update(int currentTick);
}
