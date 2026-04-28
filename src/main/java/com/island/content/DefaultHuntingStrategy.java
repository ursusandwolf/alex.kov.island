package com.island.content;

import static com.island.config.SimulationConstants.HUNT_ROI_THRESHOLD;
import static com.island.config.SimulationConstants.HUNT_STRIKE_COST_MAX_ENERGY_CAP;
import static com.island.config.SimulationConstants.HUNT_STRIKE_COST_PREY_WEIGHT_FRACTION;
import static com.island.config.SimulationConstants.PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT;

import com.island.util.InteractionMatrix;

/**
 * Default implementation of hunting logic.
 */
public class DefaultHuntingStrategy implements HuntingStrategy {
    private final InteractionMatrix interactionMatrix;

    public DefaultHuntingStrategy(InteractionMatrix interactionMatrix) {
        this.interactionMatrix = interactionMatrix;
    }

    @Override
    public double calculateSuccessRate(Animal predator, Organism prey) {
        int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
        return chance / 100.0;
    }

    @Override
    public double calculatePackSuccessRate(java.util.List<Animal> pack, Organism prey, int baseChance) {
        double bonus = pack.size() / 100.0; // 1% per wolf
        
        // Large prey bonus: grows from 2% up to 30%
        if (prey.getWeight() > 150) { // Large or Huge
            double packBonus = Math.min(com.island.config.SimulationConstants.WOLF_PACK_MAX_BONUS_PERCENT, pack.size()) / 100.0;
            bonus = Math.max(bonus, packBonus);
        }
        
        return Math.min(1.0, (baseChance / 100.0) + bonus);
    }

    @Override
    public double calculateHuntCost(Animal predator, Organism prey) {
        double preyWeight = prey.getWeight();

        // Strike effort
        double strikeCost = Math.min(preyWeight * HUNT_STRIKE_COST_PREY_WEIGHT_FRACTION, 
                                     predator.getMaxEnergy() * HUNT_STRIKE_COST_MAX_ENERGY_CAP);
        
        // Chase cost (only for animals)
        double chaseCost = 0;
        if (prey instanceof Animal a) {
            int speedDifference = a.getSpeed() - predator.getSpeed();
            if (speedDifference > 0) {
                chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
            }
        }

        return strikeCost + chaseCost;
    }

    @Override
    public boolean isWorthHunting(Animal predator, Organism prey, double successRate, double cost) {
        double preyWeight = prey.getWeight();
        double expectedGain = preyWeight * successRate;
        // ROI Check: Expected profit must be at least ROI threshold higher than effort
        return expectedGain >= cost * HUNT_ROI_THRESHOLD;
    }
}
