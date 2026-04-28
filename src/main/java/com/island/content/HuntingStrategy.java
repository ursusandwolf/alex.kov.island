package com.island.content;

import com.island.model.Cell;

/**
 * Strategy for hunting/feeding interactions between organisms.
 */
public interface HuntingStrategy {
    /**
     * Calculates the success rate (0.0 to 1.0) for a hunt attempt.
     */
    double calculateSuccessRate(Animal predator, Organism prey);

    /**
     * Calculates the success rate (0.0 to 1.0) for a pack hunt attempt.
     */
    default double calculatePackSuccessRate(java.util.List<Animal> pack, Organism prey, int baseChance) {
        return baseChance / 100.0;
    }

    /**
     * Calculates the energy cost for a hunt attempt.
     */
    double calculateHuntCost(Animal predator, Organism prey);

    /**
     * Determines if the predator is willing to hunt this prey (ROI check).
     */
    boolean isWorthHunting(Animal predator, Organism prey, double successRate, double cost);
}
