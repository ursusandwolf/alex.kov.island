package com.island.content;

import static com.island.config.SimulationConstants.PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT;

import com.island.content.animals.herbivores.Caterpillar;
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
    public double calculateHuntCost(Animal predator, Organism prey) {
        double preyWeight = prey.getWeight();

        // Strike effort
        double strikeCost = Math.min(preyWeight * 0.1, predator.getMaxEnergy() * 0.005);
        
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
        // ROI Check: Expected profit must be at least 10% higher than effort
        return expectedGain >= cost * 1.1;
    }
}
