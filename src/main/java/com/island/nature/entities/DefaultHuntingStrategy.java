package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.util.InteractionProvider;
import java.util.List;

public class DefaultHuntingStrategy implements HuntingStrategy {
    private final InteractionProvider interactionMatrix;
    private final Configuration config;

    public DefaultHuntingStrategy(Configuration config, InteractionProvider interactionMatrix) {
        this.config = config;
        this.interactionMatrix = interactionMatrix;
    }

    @Override
    public int calculateSuccessRate(Animal predator, Organism prey) {
        return interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey()) * 100;
    }

    @Override
    public int calculatePackSuccessRate(List<Animal> pack, Organism prey, int baseChancePercent) {
        int bonusBP = pack.size() * 100; // 1% per member
        
        // Coordinated bonus for large prey (e.g., Buffalo, Horse, Bear)
        if (prey.getWeight() > 150 * config.getScale1M()) { 
            int packBonusBP = Math.min(config.getWolfPackMaxBonusPercent(), pack.size()) * 100;
            bonusBP = Math.max(bonusBP, packBonusBP);
            
            // Special rule for Bear (Apex Predator) - solo chance is 0%, but pack can kill it
            if (prey.getSpeciesKey().equals(SpeciesKey.BEAR)) {
                int bearChanceBP = Math.min(config.getWolfPackBearHuntMaxChancePercent(), pack.size()) * 100;
                return Math.max(baseChancePercent * 100, bearChanceBP);
            }
        }
        
        return Math.min(config.getScale10K(), (baseChancePercent * 100) + bonusBP);
    }

    @Override
    public long calculateHuntCost(Animal predator, Organism prey) {
        long preyWeight = prey.getWeight();
        long strikeCost = Math.min((preyWeight * config.getHuntStrikeCostPreyWeightBP()) / config.getScale10K(), 
                                     (predator.getMaxEnergy() * config.getHuntStrikeCostMaxEnergyCapBP()) / config.getScale10K());
        long chaseCost = 0;
        if (prey instanceof Animal a) {
            int speedDifference = a.getSpeed() - predator.getSpeed();
            if (speedDifference > 0) {
                chaseCost = (predator.getMaxEnergy() * speedDifference * config.getPreyRelativeSpeedHuntCostStepBP()) / config.getScale10K();
            }
        }
        
        long totalCost = strikeCost + chaseCost;
        
        // Fox Special Ability: High Agility (60% energy discount on hunting)
        if (predator.getSpeciesKey().equals(SpeciesKey.FOX)) {
            totalCost = (totalCost * 4000) / config.getScale10K();
        }
        
        return totalCost;
    }

    @Override
    public boolean isWorthHunting(Animal predator, Organism prey, int successRateBP, long cost) {
        long expectedGain = (prey.getWeight() * successRateBP) / config.getScale10K();
        return expectedGain >= (cost * config.getHuntRoiThresholdBP()) / config.getScale10K();
    }

    @Override
    public Organism selectPrey(Animal predator, PreyProvider provider) {
        for (Organism prey : provider.getPreyFor(predator)) {
            int successRateBP = calculateSuccessRate(predator, prey);
            long cost = calculateHuntCost(predator, prey);
            if (isWorthHunting(predator, prey, successRateBP, cost)) {
                return prey;
            }
        }
        return null;
    }

    @Override
    public Organism selectPackPrey(List<Animal> pack, PreyProvider provider) {
        if (pack.isEmpty()) {
            return null;
        }
        Animal leader = pack.get(0);
        for (Organism prey : provider.getPreyFor(leader)) {
            int baseChance = interactionMatrix.getChance(leader.getSpeciesKey(), prey.getSpeciesKey());
            int packSuccessRateBP = calculatePackSuccessRate(pack, prey, baseChance);
            long cost = calculateHuntCost(leader, prey); // Cost for the leader's strike
            if (isWorthHunting(leader, prey, packSuccessRateBP, cost)) {
                return prey;
            }
        }
        return null;
    }
}
