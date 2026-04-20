package com.island.content;

/**
 * Abstract class for all animals (both predators and herbivores).
 * Extends Organism with animal-specific properties like speed and weight.
 * 
 * GOF Patterns:
 * - Template Method: eat(), move(), reproduce() have default implementations
 *   that subclasses can override for specific behaviors
 * 
 * GRASP Principles:
 * - Creator: Animals create other animals during reproduction
 * - Low Coupling: Depends on Cell interface, not concrete implementation
 */
public abstract class Animal extends Organism {
    
    // Weight in kilograms (from specification table)
    private final double weight;
    
    // Maximum individuals of this species per cell
    private final int maxPerCell;
    
    // Movement speed (max cells per tick)
    private final int speed;
    
    // Food needed for full saturation (in kg)
    private final double foodForSaturation;
    
    /**
     * Constructor for Animal.
     * 
     * @param weight weight in kg
     * @param maxPerCell maximum individuals per cell
     * @param speed movement speed (cells per tick)
     * @param foodForSaturation kg of food for full saturation
     * @param maxLifespan maximum lifespan in ticks
     */
    protected Animal(double weight, int maxPerCell, int speed, 
                     double foodForSaturation, int maxLifespan) {
        // Max energy is calculated as 100 * foodForSaturation (arbitrary scale)
        super(foodForSaturation * 100, maxLifespan);
        this.weight = weight;
        this.maxPerCell = maxPerCell;
        this.speed = speed;
        this.foodForSaturation = foodForSaturation;
    }
    
    /**
     * Get animal weight.
     * @return weight in kg
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
     * Default eat implementation - should be overridden by subclasses.
     * 
     * @return energy gained
     */
    @Override
    public double eat() {
        System.out.println(getTypeName() + " needs to implement eat() method");
        return 0;
    }
    
    /**
     * Default move implementation.
     * 
     * @return true if moved successfully
     */
    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }
        
        consumeEnergy(getMaxEnergy() * 0.05); // 5% energy cost
        return false; // Placeholder
    }
    
    /**
     * Default reproduce implementation.
     * Requires 2 individuals of same species in same cell.
     * 
     * @return new Animal instance or null
     */
    @Override
    public Animal reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        consumeEnergy(getMaxEnergy() * 0.05); // 5% energy cost
        return null; // Placeholder
    }
    
    /**
     * Check if this animal can eat a specific prey species.
     * Implemented using probability table from SpeciesConfig
     * 
     * @param preySpeciesKey the species key of potential prey
     * @return true if this animal can eat the prey
     */
    public abstract boolean canEat(String preySpeciesKey);
    
    /**
     * Get hunting success probability for a prey species.
     * Implemented using probability table
     * 
     * @param preySpeciesKey the species key of potential prey
     * @return probability percentage (0-100)
     */
    public abstract int getHuntProbability(String preySpeciesKey);
}
