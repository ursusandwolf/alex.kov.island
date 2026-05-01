package com.island.util;

import com.island.nature.entities.AnimalType;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance matrix of predator-prey interaction chances.
 * Uses primitive 2D array for speed, with a mapping for SpeciesKey indices.
 */
public class InteractionMatrix implements InteractionProvider {
    private final Map<SpeciesKey, Integer> indexMap;
    private final int size;
    private final SpeciesRegistry registry;
    
    private int[][] matrix;
    private boolean frozen = false;

    public InteractionMatrix(SpeciesRegistry registry) {
        this.registry = registry;
        this.indexMap = new HashMap<>();
        int i = 0;
        for (SpeciesKey key : SpeciesKey.values()) {
            indexMap.put(key, i++);
        }
        this.size = i;
        this.matrix = new int[size][size];
    }

    /**
     * Sets the success chance for a predator-prey pair.
     * 
     * @param predator the species that eats
     * @param prey the species being eaten
     * @param chance percentage (0-100)
     */
    public synchronized void setChance(SpeciesKey predator, SpeciesKey prey, int chance) {
        if (frozen) {
            // Copy-on-write if someone tries to modify a frozen matrix (mostly for tests)
            int[][] newMatrix = new int[size][size];
            for (int i = 0; i < size; i++) {
                newMatrix[i] = Arrays.copyOf(matrix[i], size);
            }
            matrix = newMatrix;
            frozen = false;
        }
        int predIdx = getIndex(predator);
        int preyIdx = getIndex(prey);
        if (predIdx != -1 && preyIdx != -1) {
            matrix[predIdx][preyIdx] = chance;
        }
    }

    /**
     * Gets the success chance for a predator-prey pair.
     */
    public int getChance(SpeciesKey predator, SpeciesKey prey) {
        int predIdx = getIndex(predator);
        int preyIdx = getIndex(prey);
        if (predIdx != -1 && preyIdx != -1) {
            return matrix[predIdx][preyIdx];
        }
        return 0;
    }

    public boolean hasAnimalPrey(SpeciesKey predator) {
        for (SpeciesKey prey : SpeciesKey.values()) {
            boolean isBiomass = registry.getAnimalType(prey).map(AnimalType::isBiomass).orElse(false);
            if (!isBiomass && getChance(predator, prey) > 0) {
                return true;
            }
        }
        return false;
    }

    private int getIndex(SpeciesKey key) {
        Integer idx = indexMap.get(key);
        return (idx != null) ? idx : -1;
    }

    /**
     * Marks the matrix as frozen. Subsequent modifications will trigger a copy.
     */
    public synchronized void freeze() {
        this.frozen = true;
    }

    public static InteractionMatrix buildFrom(SpeciesRegistry registry) {
        InteractionMatrix matrix = new InteractionMatrix(registry);
        for (SpeciesKey predatorKey : SpeciesKey.values()) {
            for (SpeciesKey preyKey : SpeciesKey.values()) {
                int chance = registry.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                    // If can eat generic PLANT, can eat any specific plant
                    if (preyKey.equals(SpeciesKey.PLANT)) {
                        for (SpeciesKey otherKey : registry.getAllBiomassKeys()) {
                            if (registry.getBiomassType(otherKey).map(AnimalType::isPlant).orElse(false)) {
                                matrix.setChance(predatorKey, otherKey, chance);
                            }
                        }
                    }
                }
            }
            // Default fallback for herbivores
            boolean isPredator = predatorKey.isPredator();
            boolean isBiomass = registry.getAnimalType(predatorKey).map(AnimalType::isBiomass).orElse(false);
            if (!isPredator && !isBiomass) {
                if (matrix.getChance(predatorKey, SpeciesKey.PLANT) == 0) {
                    matrix.setChance(predatorKey, SpeciesKey.PLANT, 100);
                    for (SpeciesKey otherKey : registry.getAllBiomassKeys()) {
                        if (registry.getBiomassType(otherKey).map(AnimalType::isPlant).orElse(false)) {
                            matrix.setChance(predatorKey, otherKey, 100);
                        }
                    }
                }
            }
        }
        matrix.freeze();
        return matrix;
    }
}
