package com.island.content;

import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import java.util.*;

/**
 * Optimized food provider that simulates a mixed "buffet" with competition.
 * Uses pre-calculated protection map for endangered species.
 * Includes both individual animals and biomass "portions" (like Caterpillars).
 */
public class PreyProvider {
    private final List<Organism> masterPool;
    private final InteractionMatrix matrix;
    private final int currentTick;
    private final Island island;
    private double scarcityFactor = 1.0;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int currentTick, Map<String, Double> protectionMap) {
        this.matrix = matrix;
        this.currentTick = currentTick;
        this.island = cell.getIsland();
        
        this.masterPool = new ArrayList<>();
        
        // 1. Add Animals
        List<Animal> cellAnimals = cell.getAnimals();
        cell.getLock().lock();
        try {
            for (int i = 0; i < cellAnimals.size(); i++) {
                Animal a = cellAnimals.get(i);
                if (a.isAlive() && !a.isProtected(currentTick)) {
                    Double hideChance = protectionMap.get(a.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) {
                        a.setHiding(true);
                        continue; 
                    }
                    masterPool.add(a);
                }
            }
            
            // 2. Add Caterpillar "Portions" (Virtual prey encounters)
            for (com.island.content.plants.Plant p : cell.getPlants()) {
                if (p instanceof com.island.content.animals.herbivores.Caterpillar && p.isAlive()) {
                    // Check protection (Smart Biomass hiding)
                    Double hideChance = protectionMap.get(p.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    // Add up to 10 virtual "encounters" with caterpillars
                    for (int j = 0; j < 10; j++) {
                        masterPool.add(p);
                    }
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

    public Iterable<Organism> getPreyFor(Animal predator) {
        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
        if (foodNeeded <= 0 || masterPool.isEmpty()) return Collections.emptyList();

        String predKey = predator.getSpeciesKey();
        
        // --- Predator Intelligence and Persistence ---
        // Base quota of search attempts
        int baseQuota = 15 + (int)(foodNeeded * 2); 
        
        // Gradual persistence bonus based on size (instead of sharp 5x jump)
        if (predator.getWeight() > 100) {
            baseQuota += (int)(predator.getWeight() / 10.0);
        }

        // Limit maximum search depth to prevent wiping out entire cells
        if (baseQuota > 60) baseQuota = 60;

        // Scarcity impact softened: predators always get at least 60% of their quota
        int limitedQuota = (int) (baseQuota * (0.6 + (scarcityFactor * 0.4))); 
        
        // Minimum search depth: 5 attempts if pool is large
        if (limitedQuota < 5 && masterPool.size() > 10) limitedQuota = 5;

        List<Organism> buffet = new ArrayList<>();
        int attempts = 0;

        for (int i = 0; i < masterPool.size(); i++) {
            Organism potentialPrey = masterPool.get(i);
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

    public void markAsEaten(Organism prey) {
        masterPool.remove(prey);
    }
}
