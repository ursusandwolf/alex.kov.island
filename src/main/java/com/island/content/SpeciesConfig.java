package com.island.content;

import lombok.Getter;
import java.util.*;

// Конфигуратор видов (Singleton)
@Getter
public final class SpeciesConfig {
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    private final Map<String, AnimalType> animalTypes = new HashMap<>();

    public static SpeciesConfig getInstance() { return INSTANCE; }

    private SpeciesConfig() {
        // Временно оставляем упрощенную инициализацию, избегая огромного конструктора
        initPredators();
        initHerbivores();
    }

    private void initPredators() {
        animalTypes.put("wolf", new AnimalType("wolf", "Wolf", 50, 30, 3, 8, 10000, Map.of("rabbit", 60, "duck", 40)));
        animalTypes.put("fox", new AnimalType("fox", "Fox", 8, 30, 2, 2, 10000, Map.of("rabbit", 70, "duck", 50)));
    }

    private void initHerbivores() {
        animalTypes.put("rabbit", new AnimalType("rabbit", "Rabbit", 2, 150, 2, 0.45, 10000, null));
        animalTypes.put("duck", new AnimalType("duck", "Duck", 1, 200, 4, 0.15, 10000, Map.of("caterpillar", 90)));
        animalTypes.put("caterpillar", new AnimalType("caterpillar", "Caterpillar", 0.01, 1000, 0, 0, Integer.MAX_VALUE, null));
    }

    public AnimalType getAnimalType(String key) { return animalTypes.get(key); }
}
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
