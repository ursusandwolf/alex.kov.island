package com.island.util;

import com.island.content.SpeciesKey;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * High-performance matrix of predator-prey interaction chances.
 * Uses primitive 2D array for speed, with a mapping for SpeciesKey indices.
 */
public class InteractionMatrix implements InteractionProvider {
    private static final Map<SpeciesKey, Integer> INDEX_MAP = new HashMap<>();
    private static final int SIZE;
    
    static {
        int i = 0;
        for (SpeciesKey key : SpeciesKey.values()) {
            INDEX_MAP.put(key, i++);
        }
        SIZE = i;
    }

    private int[][] matrix = new int[SIZE][SIZE];
    private boolean frozen = false;

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
            int[][] newMatrix = new int[SIZE][SIZE];
            for (int i = 0; i < SIZE; i++) {
                newMatrix[i] = Arrays.copyOf(matrix[i], SIZE);
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
            if (!prey.isBiomass() && getChance(predator, prey) > 0) {
                return true;
            }
        }
        return false;
    }

    private int getIndex(SpeciesKey key) {
        Integer idx = INDEX_MAP.get(key);
        return (idx != null) ? idx : -1;
    }

    /**
     * Marks the matrix as frozen. Subsequent modifications will trigger a copy.
     */
    public synchronized void freeze() {
        this.frozen = true;
    }

    public static InteractionMatrix buildFrom(com.island.content.SpeciesRegistry registry) {
        InteractionMatrix matrix = new InteractionMatrix();
        for (com.island.content.SpeciesKey predatorKey : com.island.content.SpeciesKey.values()) {
            for (com.island.content.SpeciesKey preyKey : com.island.content.SpeciesKey.values()) {
                int chance = registry.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                    // If can eat generic PLANT, can eat any specific plant
                    if (preyKey.equals(com.island.content.SpeciesKey.PLANT)) {
                        matrix.setChance(predatorKey, com.island.content.SpeciesKey.GRASS, chance);
                        matrix.setChance(predatorKey, com.island.content.SpeciesKey.CABBAGE, chance);
                    }
                }
            }
            // Default fallback for herbivores
            if (!predatorKey.isPredator() && !predatorKey.isBiomass()) {
                if (matrix.getChance(predatorKey, com.island.content.SpeciesKey.PLANT) == 0) {
                    matrix.setChance(predatorKey, com.island.content.SpeciesKey.PLANT, 100);
                    matrix.setChance(predatorKey, com.island.content.SpeciesKey.GRASS, 100);
                    matrix.setChance(predatorKey, com.island.content.SpeciesKey.CABBAGE, 100);
                }
            }
        }
        matrix.freeze();
        return matrix;
    }
}
