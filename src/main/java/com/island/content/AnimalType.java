package com.island.content;

/**
 * Flyweight class containing intrinsic state shared across all animals of the same species.
 * This optimization reduces memory usage when creating thousands of animal instances.
 * 
 * GOF Patterns:
 * - Flyweight: shares common state across multiple objects
 * 
 * GRASP Principles:
 * - Information Expert: contains all species-specific configuration data
 * - Low Coupling: separates intrinsic (shared) from extrinsic (instance) state
 */
public final class AnimalType {
    
    // Intrinsic state - shared across all instances of this species
    private final String speciesKey;
    private final String typeName;
    private final double weight;
    private final int maxPerCell;
    private final int speed;
    private final double foodForSaturation;
    private final int maxLifespan;
    private final double maxEnergy;
    
    // Hunting probability matrix for this species (if predator)
    private final java.util.Map<String, Integer> huntProbabilities;
    private final boolean isPredator;
    
    /**
     * Constructor for AnimalType with all intrinsic state.
     * 
     * @param speciesKey unique species identifier
     * @param typeName display name
     * @param weight weight in kg
     * @param maxPerCell maximum individuals per cell
     * @param speed movement speed (cells per tick)
     * @param foodForSaturation kg of food for full saturation
     * @param maxLifespan maximum lifespan in ticks
     * @param huntProbabilities hunting probabilities map (empty for herbivores)
     */
    public AnimalType(String speciesKey, String typeName, double weight, int maxPerCell,
                      int speed, double foodForSaturation, int maxLifespan,
                      java.util.Map<String, Integer> huntProbabilities) {
        this.speciesKey = speciesKey;
        this.typeName = typeName;
        this.weight = weight;
        this.maxPerCell = maxPerCell;
        this.speed = speed;
        this.foodForSaturation = foodForSaturation;
        this.maxLifespan = maxLifespan;
        this.maxEnergy = foodForSaturation * 100; // Calculated from food needs
        this.huntProbabilities = huntProbabilities != null ? 
                java.util.Collections.unmodifiableMap(new java.util.HashMap<>(huntProbabilities)) :
                java.util.Collections.emptyMap();
        this.isPredator = !this.huntProbabilities.isEmpty();
    }
    
    /**
     * Get species key.
     * @return species identifier
     */
    public String getSpeciesKey() {
        return speciesKey;
    }
    
    /**
     * Get type name for display.
     * @return type name
     */
    public String getTypeName() {
        return typeName;
    }
    
    /**
     * Get weight in kg.
     * @return weight
     */
    public double getWeight() {
        return weight;
    }
    
    /**
     * Get maximum individuals per cell.
     * @return max count
     */
    public int getMaxPerCell() {
        return maxPerCell;
    }
    
    /**
     * Get movement speed.
     * @return cells per tick
     */
    public int getSpeed() {
        return speed;
    }
    
    /**
     * Get food needed for saturation.
     * @return kg of food
     */
    public double getFoodForSaturation() {
        return foodForSaturation;
    }
    
    /**
     * Get maximum lifespan.
     * @return ticks
     */
    public int getMaxLifespan() {
        return maxLifespan;
    }
    
    /**
     * Get maximum energy capacity.
     * @return max energy
     */
    public double getMaxEnergy() {
        return maxEnergy;
    }
    
    /**
     * Check if this species is a predator.
     * @return true if predator
     */
    public boolean isPredator() {
        return isPredator;
    }
    
    /**
     * Check if can eat specific prey.
     * @param preySpeciesKey prey species
     * @return true if can eat
     */
    public boolean canEat(String preySpeciesKey) {
        return huntProbabilities.containsKey(preySpeciesKey);
    }
    
    /**
     * Get hunting success probability.
     * @param preySpeciesKey prey species
     * @return probability percentage (0-100)
     */
    public int getHuntProbability(String preySpeciesKey) {
        return huntProbabilities.getOrDefault(preySpeciesKey, 0);
    }
    
    /**
     * Get all prey species this animal can eat.
     * @return set of prey species keys
     */
    public java.util.Set<String> getPreySpecies() {
        return huntProbabilities.keySet();
    }
}
