package com.island.content;

import static com.island.config.SimulationConstants.HUNT_ROI_THRESHOLD_BP;
import static com.island.config.SimulationConstants.HUNT_STRIKE_COST_MAX_ENERGY_CAP_BP;
import static com.island.config.SimulationConstants.HUNT_STRIKE_COST_PREY_WEIGHT_BP;
import static com.island.config.SimulationConstants.PREY_RELATIVE_SPEED_HUNT_COST_STEP_BP;
import static com.island.config.SimulationConstants.SCALE_10K;
import static com.island.config.SimulationConstants.SCALE_1M;
import static com.island.config.SimulationConstants.WOLF_PACK_MAX_BONUS_PERCENT;

import com.island.util.InteractionProvider;
import java.util.List;

public class DefaultHuntingStrategy implements HuntingStrategy {
    private final InteractionProvider interactionMatrix;

    public DefaultHuntingStrategy(InteractionProvider interactionMatrix) {
        this.interactionMatrix = interactionMatrix;
    }

    @Override
    public int calculateSuccessRate(Animal predator, Organism prey) {
        return interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey()) * 100;
    }

    @Override
    public int calculatePackSuccessRate(List<Animal> pack, Organism prey, int baseChancePercent) {
        int bonusBP = pack.size() * 100; 
        if (prey.getWeight() > 150 * SCALE_1M) { 
            int packBonusBP = Math.min(WOLF_PACK_MAX_BONUS_PERCENT, pack.size()) * 100;
            bonusBP = Math.max(bonusBP, packBonusBP);
        }
        return Math.min(SCALE_10K, (baseChancePercent * 100) + bonusBP);
    }

    @Override
    public long calculateHuntCost(Animal predator, Organism prey) {
        long preyWeight = prey.getWeight();
        long strikeCost = Math.min((preyWeight * HUNT_STRIKE_COST_PREY_WEIGHT_BP) / SCALE_10K, 
                                     (predator.getMaxEnergy() * HUNT_STRIKE_COST_MAX_ENERGY_CAP_BP) / SCALE_10K);
        long chaseCost = 0;
        if (prey instanceof Animal a) {
            int speedDifference = a.getSpeed() - predator.getSpeed();
            if (speedDifference > 0) {
                chaseCost = (predator.getMaxEnergy() * speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_BP) / SCALE_10K;
            }
        }
        return strikeCost + chaseCost;
    }

    @Override
    public boolean isWorthHunting(Animal predator, Organism prey, int successRateBP, long cost) {
        long expectedGain = (prey.getWeight() * successRateBP) / SCALE_10K;
        return expectedGain >= (cost * HUNT_ROI_THRESHOLD_BP) / SCALE_10K;
    }

    @Override
    public Organism selectPrey(Animal predator, PreyProvider provider) {
        for (Organism prey : provider.getPreyFor(predator)) {
            int successRate = calculateSuccessRate(predator, prey);
            long cost = calculateHuntCost(predator, prey);
            if (isWorthHunting(predator, prey, successRate, cost)) return prey;
        }
        return null;
    }
}
