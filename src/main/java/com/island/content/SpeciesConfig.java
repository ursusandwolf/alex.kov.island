package com.island.content;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central configuration class for all species parameters.
 * Uses Singleton pattern to ensure single source of truth.
 * 
 * Contains:
 * - Species characteristics (weight, speed, capacity, food needs)
 * - Hunting probability matrix
 * - Lifespan settings
 * 
 * GOF Patterns:
 * - Singleton: single instance with global access
 * - Strategy: probability calculations could be swapped
 * 
 * GRASP Principles:
 * - Information Expert: knows all species data
 * - Low Coupling: other classes depend on this config, not hardcoded values
 */
public final class SpeciesConfig {
    
    // Singleton instance
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    
    /**
     * Get singleton instance.
     * @return the single SpeciesConfig instance
     */
    public static SpeciesConfig getInstance() {
        return INSTANCE;
    }
    
    // Private constructor prevents instantiation
    private SpeciesConfig() {
        initializeSpeciesData();
        initializeProbabilityMatrix();
    }
    
    // =========================================================================
    // SPECIES CHARACTERISTICS
    // Format: speciesKey -> {weight, maxPerCell, speed, foodForSaturation}
    // =========================================================================
    
    /**
     * Inner class to hold species characteristics.
     * Immutable design for thread safety.
     */
    public static final class SpeciesCharacteristics {
        private final double weight;
        private final int maxPerCell;
        private final int speed;
        private final double foodForSaturation;
        private final int maxLifespan;
        
        public SpeciesCharacteristics(double weight, int maxPerCell, int speed,
                                      double foodForSaturation, int maxLifespan) {
            this.weight = weight;
            this.maxPerCell = maxPerCell;
            this.speed = speed;
            this.foodForSaturation = foodForSaturation;
            this.maxLifespan = maxLifespan;
        }
        
        public double getWeight() { return weight; }
        public int getMaxPerCell() { return maxPerCell; }
        public int getSpeed() { return speed; }
        public double getFoodForSaturation() { return foodForSaturation; }
        public int getMaxLifespan() { return maxLifespan; }
    }
    
    // Storage for species characteristics
    private Map<String, SpeciesCharacteristics> speciesData;
    
    // =========================================================================
    // HUNTING PROBABILITY MATRIX
    // predatorKey -> preyKey -> probability (0-100)
    // =========================================================================
    
    private Map<String, Map<String, Integer>> huntProbabilities;
    
    /**
     * Initialize species data from specification table.
     */
    private void initializeSpeciesData() {
        speciesData = new HashMap<>();
        
        // PREDATORS (5 species)
        // Wolf: 50kg, 30 per cell, speed 3, 8kg food, 10000 lifespan
        speciesData.put("wolf", new SpeciesCharacteristics(50, 30, 3, 8, 10000));
        
        // HERBIVORES (10 species)
        // Horse: 400kg, 20 per cell, speed 4, 60kg food, 10000 lifespan
        speciesData.put("horse", new SpeciesCharacteristics(400, 20, 4, 60, 10000));
        
        // Rabbit: 2kg, 150 per cell, speed 2, 0.45kg food, 10000 lifespan
        speciesData.put("rabbit", new SpeciesCharacteristics(2, 150, 2, 0.45, 10000));
        
        // Duck: 1kg, 200 per cell, speed 4, 0.15kg food, 10000 lifespan
        speciesData.put("duck", new SpeciesCharacteristics(1, 200, 4, 0.15, 10000));
        
        // Caterpillar: 0.01kg, 1000 per cell, speed 0, 0kg food, immortal
        speciesData.put("caterpillar", new SpeciesCharacteristics(0.01, 1000, 0, 0, Integer.MAX_VALUE));
        
        // PLANTS
        // Plants: 1kg biomass, 200 per cell, N/A speed, N/A food, immortal
        speciesData.put("plant", new SpeciesCharacteristics(1, 200, 0, 0, 0));
    }
    
    /**
     * Initialize hunting probability matrix.
     */
    private void initializeProbabilityMatrix() {
        huntProbabilities = new HashMap<>();
        
        // Wolf probabilities
        Map<String, Integer> wolfPrefs = new HashMap<>();
        wolfPrefs.put("horse", 10);
        wolfPrefs.put("deer", 15);
        wolfPrefs.put("rabbit", 60);
        wolfPrefs.put("mouse", 80);
        wolfPrefs.put("goat", 60);
        wolfPrefs.put("sheep", 70);
        wolfPrefs.put("wild_boar", 15);
        wolfPrefs.put("buffalo", 10);
        wolfPrefs.put("duck", 40);
        
        huntProbabilities.put("wolf", wolfPrefs);
        
        // Duck probabilities (can eat caterpillars)
        Map<String, Integer> duckPrefs = new HashMap<>();
        duckPrefs.put("caterpillar", 90);
        huntProbabilities.put("duck", duckPrefs);
    }
    
    /**
     * Get characteristics for a species.
     * 
     * @param speciesKey the species identifier
     * @return SpeciesCharacteristics or null if not found
     */
    public SpeciesCharacteristics getCharacteristics(String speciesKey) {
        return speciesData.get(speciesKey);
    }
    
    /**
     * Get hunting probability for a predator-prey pair.
     * 
     * @param predatorKey predator species
     * @param preyKey prey species
     * @return probability percentage (0 if cannot eat)
     */
    public int getHuntProbability(String predatorKey, String preyKey) {
        Map<String, Integer> prefs = huntProbabilities.get(predatorKey);
        if (prefs == null) {
            return 0;
        }
        return prefs.getOrDefault(preyKey, 0);
    }
    
    /**
     * Check if predator can eat prey (probability > 0).
     * 
     * @param predatorKey predator species
     * @param preyKey prey species
     * @return true if can eat
     */
    public boolean canEat(String predatorKey, String preyKey) {
        return getHuntProbability(predatorKey, preyKey) > 0;
    }
    
    /**
     * Roll for successful hunt using multithreaded random.
     * 
     * @param predatorKey predator species
     * @param preyKey prey species
     * @return true if hunt succeeds
     */
    public boolean rollHuntSuccess(String predatorKey, String preyKey) {
        int probability = getHuntProbability(predatorKey, preyKey);
        if (probability <= 0) {
            return false;
        }
        // ThreadLocalRandom is thread-safe for multithreaded simulation
        int roll = ThreadLocalRandom.current().nextInt(101); // 0-100
        return roll < probability;
    }
    
    /**
     * Get all species keys.
     * @return set of species keys
     */
    public Set<String> getAllSpeciesKeys() {
        return speciesData.keySet();
    }
    
    /**
     * Check if species is a predator.
     * 
     * @param speciesKey the species
     * @return true if predator
     */
    public boolean isPredator(String speciesKey) {
        return huntProbabilities.containsKey(speciesKey);
    }
}
