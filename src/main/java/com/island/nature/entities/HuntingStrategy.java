package com.island.nature.entities;

import java.util.List;

/**
 * Strategy for hunting/feeding interactions between organisms using integer arithmetic.
 */
public interface HuntingStrategy {
    /**
     * Calculates the success rate in basis points (0-10000) for a hunt attempt.
     */
    int calculateSuccessRate(Animal predator, Organism prey);

    /**
     * Calculates the success rate in basis points (0-10000) for a pack hunt attempt.
     */
    default int calculatePackSuccessRate(List<Animal> pack, Organism prey, int baseChancePercent) {
        return baseChancePercent * 100;
    }

    /**
     * Calculates the energy cost for a hunt attempt (SCALE_1M).
     */
    long calculateHuntCost(Animal predator, Organism prey);

    /**
     * Determines if the predator is willing to hunt this prey (ROI check).
     */
    boolean isWorthHunting(Animal predator, Organism prey, int successRateBP, long cost);

    /**
     * Selects the best prey from the available providers.
     */
    Organism selectPrey(Animal predator, PreyProvider provider);

    /**
     * Selects the best prey for a pack of predators.
     */
    Organism selectPackPrey(List<Animal> pack, PreyProvider provider);
}
