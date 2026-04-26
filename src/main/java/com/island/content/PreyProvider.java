package com.island.content;

import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Optimized food provider that simulates a mixed "buffet" with competition.
 * Instead of providing specific species, it gives a randomized mixed stream 
 * of potential prey based on cell-wide scarcity and predator initiative.
 */
public class PreyProvider {
    private final List<Animal> masterPool;
    private final InteractionMatrix matrix;
    private final int currentTick;
    private double scarcityFactor = 1.0;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int currentTick) {
        this.matrix = matrix;
        this.currentTick = currentTick;
        
        // Use a simple loop instead of Streams for grouping and filtering
        this.masterPool = new ArrayList<>();
        List<Animal> cellAnimals = cell.getAnimals();
        cell.getLock().lock();
        try {
            for (Animal a : cellAnimals) {
                if (a.isAlive() && !a.isProtected(currentTick)) {
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
            for (Animal a : cellAnimals) {
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

    /**
     * Provides a mixed batch of potential prey.
     * Quota is influenced by scarcity (competition) and predator's individual needs.
     */
    public Iterable<Animal> getPreyFor(Animal predator) {
        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
        if (foodNeeded <= 0 || masterPool.isEmpty()) return Collections.emptyList();

        String predKey = predator.getSpeciesKey();
        
        // Calculate quota of "search attempts".
        // A predator doesn't get to see all prey; it gets a "slice" of the mixed pool.
        int baseQuota = 5 + (int)(foodNeeded * 2); 
        int limitedQuota = (int) (baseQuota * scarcityFactor);
        
        // Minimum 1 attempt if there is any food at all
        if (limitedQuota < 1 && scarcityFactor > 0.05) limitedQuota = 1;

        List<Animal> buffet = new ArrayList<>();
        int attempts = 0;

        // Iterate through the shuffled pool and pick anything this predator can eat
        for (Animal potentialPrey : masterPool) {
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
