package com.island.content;

/**
 * Abstract class for plants in the simulation.
 * Plants are simpler than animals - they grow and can be eaten.
 * 
 * GOF Patterns:
 * - Template Method: grow() method can be overridden for different plant types
 * 
 * GRASP Principles:
 * - Information Expert: Plant manages its own biomass
 */
public abstract class Plant extends Organism {
    
    // Current biomass in kg (acts as "health" and food value)
    protected double biomass;
    
    // Maximum biomass this plant can reach
    protected final double maxBiomass;
    
    // Growth rate per tick (kg)
    protected final double growthRate;
    
    /**
     * Constructor for Plant.
     * 
     * @param maxBiomass maximum biomass in kg
     * @param growthRate growth per tick in kg
     * @param maxLifespan maximum lifespan (0 for immortal like grass)
     */
    protected Plant(double maxBiomass, double growthRate, int maxLifespan) {
        super(maxBiomass * 100, maxLifespan); // Energy scaled to biomass
        this.maxBiomass = maxBiomass;
        this.biomass = maxBiomass * 0.5; // Start at 50% biomass
        this.growthRate = growthRate;
    }
    
    /**
     * Get current biomass.
     * @return biomass in kg
     */
    public double getBiomass() {
        return biomass;
    }
    
    /**
     * Get maximum biomass.
     * @return max biomass
     */
    public double getMaxBiomass() {
        return maxBiomass;
    }
    
    /**
     * Reduce biomass when eaten.
     * 
     * @param amount kg to remove
     * @return actual amount removed (may be less if not enough biomass)
     */
    public double consumeBiomass(double amount) {
        double actualAmount = Math.min(biomass, amount);
        biomass -= actualAmount;
        if (biomass <= 0) {
            die();
        }
        return actualAmount;
    }
    
    /**
     * Grow plant biomass.
     * Default implementation adds growthRate up to maxBiomass.
     * TODO: Override for different plant types with seasonal growth
     */
    public void grow() {
        if (!isAlive()) {
            return;
        }
        biomass = Math.min(maxBiomass, biomass + growthRate);
    }
    
    /**
     * Plants don't eat - always return 0.
     */
    @Override
    public double eat() {
        return 0; // Plants don't eat
    }
    
    /**
     * Plants don't move.
     */
    @Override
    public boolean move() {
        return false; // Plants are stationary
    }
    
    /**
     * Plant reproduction - creates new plant nearby.
     * TODO: Implement spreading mechanism
     */
    @Override
    public Plant reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        // TODO: Implement plant reproduction
        // Could spread to adjacent cells or create offspring in same cell
        
        return null; // Placeholder
    }
}
