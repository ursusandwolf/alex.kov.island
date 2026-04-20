package com.island.content;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central configuration class for all species parameters.
 * Uses Singleton pattern to ensure single source of truth.
 * Implements Flyweight pattern through AnimalType registry.
 * 
 * Contains:
 * - Species characteristics (weight, speed, capacity, food needs)
 * - Hunting probability matrix
 * - Lifespan settings
 * - Flyweight AnimalType instances
 * 
 * GOF Patterns:
 * - Singleton: single instance with global access
 * - Flyweight: shares AnimalType instances across all animals of same species
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
        initializeAnimalTypes();
        initializeProbabilityMatrix();
    }
    
    // =========================================================================
    // FLYWEIGHT REGISTRY - AnimalType instances
    // =========================================================================
    
    private Map<String, AnimalType> animalTypes;
    
    /**
     * Initialize flyweight AnimalType registry.
     * All animals of the same species share the same AnimalType instance.
     */
    private void initializeAnimalTypes() {
        animalTypes = new HashMap<>();
        
        // PREDATORS
        // Wolf: 50kg, 30 per cell, speed 3, 8kg food, 10000 lifespan
        Map<String, Integer> wolfHunt = new HashMap<>();
        wolfHunt.put("horse", 10);
        wolfHunt.put("deer", 15);
        wolfHunt.put("rabbit", 60);
        wolfHunt.put("mouse", 80);
        wolfHunt.put("goat", 60);
        wolfHunt.put("sheep", 70);
        wolfHunt.put("wild_boar", 15);
        wolfHunt.put("buffalo", 10);
        wolfHunt.put("duck", 40);
        animalTypes.put("wolf", new AnimalType("wolf", "Wolf", 50, 30, 3, 8, 10000, wolfHunt));
        
        // Fox: 8kg, 30 per cell, speed 2, 2kg food, 10000 lifespan
        Map<String, Integer> foxHunt = new HashMap<>();
        foxHunt.put("rabbit", 70);
        foxHunt.put("mouse", 90);
        foxHunt.put("duck", 50);
        foxHunt.put("caterpillar", 95);
        animalTypes.put("fox", new AnimalType("fox", "Fox", 8, 30, 2, 2, 10000, foxHunt));
        
        // HERBIVORES
        // Horse: 400kg, 20 per cell, speed 4, 60kg food, 10000 lifespan
        animalTypes.put("horse", new AnimalType("horse", "Horse", 400, 20, 4, 60, 10000, null));
        
        // Rabbit: 2kg, 150 per cell, speed 2, 0.45kg food, 10000 lifespan
        animalTypes.put("rabbit", new AnimalType("rabbit", "Rabbit", 2, 150, 2, 0.45, 10000, null));
        
        // Duck: 1kg, 200 per cell, speed 4, 0.15kg food, 10000 lifespan
        // Special case: duck can eat caterpillars (handled separately)
        Map<String, Integer> duckHunt = new HashMap<>();
        duckHunt.put("caterpillar", 90);
        animalTypes.put("duck", new AnimalType("duck", "Duck", 1, 200, 4, 0.15, 10000, duckHunt));
        
        // Caterpillar: 0.01kg, 1000 per cell, speed 0, 0kg food, immortal
        animalTypes.put("caterpillar", new AnimalType("caterpillar", "Caterpillar", 0.01, 1000, 0, 0, Integer.MAX_VALUE, null));
        
        // PLANTS
        // Plants: 1kg biomass, 200 per cell, N/A speed, N/A food, immortal
        animalTypes.put("plant", new AnimalType("plant", "Plant", 1, 200, 0, 0, 0, null));
    }
    
    // =========================================================================
    // HUNTING PROBABILITY MATRIX (legacy support)
    // predatorKey -> preyKey -> probability (0-100)
    // =========================================================================
    
    private Map<String, Map<String, Integer>> huntProbabilities;
    
    // Legacy field for backward compatibility - can be removed in future
    @Deprecated
    private Map<String, SpeciesCharacteristics> speciesData;
    
    /**
     * Inner class to hold species characteristics (legacy).
     * Immutable design for thread safety.
     * @deprecated Use AnimalType instead
     */
    @Deprecated
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
    
    /**
     * Initialize hunting probability matrix (legacy support).
     * Now delegates to AnimalType flyweight objects.
     */
    private void initializeProbabilityMatrix() {
        // Legacy map for backward compatibility
        // New code should use getAnimalType() instead
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
        
        // Fox probabilities
        Map<String, Integer> foxPrefs = new HashMap<>();
        foxPrefs.put("rabbit", 70);
        foxPrefs.put("mouse", 90);
        foxPrefs.put("duck", 50);
        foxPrefs.put("caterpillar", 95);
        huntProbabilities.put("fox", foxPrefs);
        
        // Duck probabilities (can eat caterpillars)
        Map<String, Integer> duckPrefs = new HashMap<>();
        duckPrefs.put("caterpillar", 90);
        huntProbabilities.put("duck", duckPrefs);
    }
    
    // =========================================================================
    // FLYWEIGHT ACCESS METHODS
    // =========================================================================
    
    /**
     * Get the flyweight AnimalType for a species.
     * All animals of the same species share this instance.
     * 
     * @param speciesKey the species identifier
     * @return AnimalType or null if not found
     */
    public AnimalType getAnimalType(String speciesKey) {
        return animalTypes.get(speciesKey);
    }
    
    /**
     * Check if species exists in the registry.
     * 
     * @param speciesKey the species
     * @return true if registered
     */
    public boolean hasSpecies(String speciesKey) {
        return animalTypes.containsKey(speciesKey);
    }
    
    /**
     * Get all registered species keys.
     * @return set of species keys
     */
    public Set<String> getAllSpeciesKeys() {
        return animalTypes.keySet();
    }
    
    /**
     * Check if species is a predator using flyweight data.
     * 
     * @param speciesKey the species
     * @return true if predator
     */
    public boolean isPredator(String speciesKey) {
        AnimalType type = animalTypes.get(speciesKey);
        return type != null && type.isPredator();
    }
    
    // =========================================================================
    // LEGACY METHODS (for backward compatibility)
    // =========================================================================
    
    /**
     * Get characteristics for a species (legacy method).
     * Use getAnimalType() instead for Flyweight pattern.
     * 
     * @param speciesKey the species identifier
     * @return SpeciesCharacteristics or null if not found
     * @deprecated Use getAnimalType(speciesKey) instead
     */
    @Deprecated
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
     * Get all species keys (legacy method).
     * @return set of species keys
     * @deprecated Use getAllSpeciesKeys() which now returns from animalTypes
     */
    @Deprecated
    public Set<String> getLegacySpeciesKeys() {
        return speciesData != null ? speciesData.keySet() : java.util.Collections.emptySet();
    }
    
    /**
     * Check if species is a predator (legacy method).
     * 
     * @param speciesKey the species
     * @return true if predator
     * @deprecated Use isPredator(speciesKey) which uses flyweight data
     */
    @Deprecated
    public boolean isLegacyPredator(String speciesKey) {
        return huntProbabilities.containsKey(speciesKey);
    }
}
