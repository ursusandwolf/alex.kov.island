package com.island.content;

import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import java.util.*;

/**
 * Optimized food provider that simulates a mixed "buffet" with competition.
 * Uses pre-calculated protection map for endangered species.
 */
public class PreyProvider {
    private final List<Animal> masterPool;
    private final InteractionMatrix matrix;
    private final int currentTick;
    private final Island island;
    private double scarcityFactor = 1.0;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int currentTick, Map<String, Double> protectionMap) {
        this.matrix = matrix;
        this.currentTick = currentTick;
        this.island = cell.getIsland();
        
        this.masterPool = new ArrayList<>();
        List<Animal> cellAnimals = cell.getAnimals();
        cell.getLock().lock();
        try {
            for (int i = 0; i < cellAnimals.size(); i++) {
                Animal a = cellAnimals.get(i);
                if (a.isAlive() && !a.isProtected(currentTick)) {
                    // --- Applied Centralized Protection ---
                    Double hideChance = protectionMap.get(a.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) {
                        a.setHiding(true);
                        continue; 
                    }
                    masterPool.add(a);
                }
            }
        } finally {
            cell.getLock().unlock();
        }
        Collections.shuffle(masterPool);

        calculateScarcity(cell);
    }

    /**
     * Calculates the supply/demand ratio in the cell.
     */
    private void calculateScarcity(Cell cell) {
        double totalNeeds = 0;
        double totalSupply = 0;

        List<Animal> cellAnimals = cell.getAnimals();
        cell.getLock().lock();
        try {
            for (int i = 0; i < cellAnimals.size(); i++) {
                Animal a = cellAnimals.get(i);
                if (!a.isAlive()) continue;
                
                if (a.isAnimalPredator()) {
                    double hunger = a.getFoodForSaturation() - a.getCurrentEnergy();
                    if (hunger > 0) totalNeeds += hunger;
                } else {
                    totalSupply += a.getWeight();
                }
            }
        } finally {
            cell.getLock().unlock();
        }

        if (totalNeeds > 0) {
            this.scarcityFactor = Math.min(1.0, totalSupply / totalNeeds);
        }
    }

    public Iterable<Animal> getPreyFor(Animal predator) {
        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
        if (foodNeeded <= 0 || masterPool.isEmpty()) return Collections.emptyList();

        String predKey = predator.getSpeciesKey();
        
        int baseQuota = 5 + (int)(foodNeeded * 2); 
        int limitedQuota = (int) (baseQuota * scarcityFactor);
        
        if (limitedQuota < 1 && scarcityFactor > 0.05) limitedQuota = 1;

        List<Animal> buffet = new ArrayList<>();
        int attempts = 0;

        for (int i = 0; i < masterPool.size(); i++) {
            Animal potentialPrey = masterPool.get(i);
            if (potentialPrey == predator || !potentialPrey.isAlive()) continue;

            if (matrix.getChance(predKey, potentialPrey.getSpeciesKey()) > 0) {
                buffet.add(potentialPrey);
                if (++attempts >= limitedQuota) break;
            }
        }

        return buffet;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
        masterPool.remove(prey);
    }

    public void markAsEaten(Animal prey) {
        masterPool.remove(prey);
    }
}
