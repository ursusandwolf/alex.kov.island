package com.island.content;

import java.util.UUID;

/**
 * Abstract base class for all living organisms in the simulation.
 * Implements common properties and uses Template Method pattern for behaviors.
 * 
 * GOF Patterns:
 * - Template Method: abstract methods define the skeleton, subclasses implement details
 * - Strategy: behavior could be swapped via composition (future extension)
 * 
 * GRASP Principles:
 * - Information Expert: organism manages its own state
 * - High Cohesion: all organism-related data is here
 */
public abstract class Organism implements OrganismBehavior {
    
    // Unique identifier for each organism instance
    private final String id;
    
    // Current energy level (0 to maxEnergy)
    private double currentEnergy;
    
    // Maximum energy capacity for this organism type
    private final double maxEnergy;
    
    // Age in simulation ticks
    private int age;
    
    // Maximum lifespan in ticks (0 means immortal)
    private final int maxLifespan;
    
    // Whether organism is alive
    private boolean isAlive;
    
    /**
     * Constructor initializes organism with full energy.
     * 
     * @param maxEnergy maximum energy capacity
     * @param maxLifespan maximum lifespan in ticks (0 for immortal)
     */
    protected Organism(double maxEnergy, int maxLifespan) {
        this.id = UUID.randomUUID().toString();
        this.maxEnergy = maxEnergy;
        this.currentEnergy = maxEnergy; // Start with full energy
        this.age = 0;
        this.maxLifespan = maxLifespan;
        this.isAlive = true;
    }
    
    /**
     * Get unique organism ID.
     * @return UUID string
     */
    public String getId() {
        return id;
    }
    
    /**
     * Check if organism is still alive.
     * @return true if alive
     */
    public boolean isAlive() {
        return isAlive;
    }
    
    /**
     * Mark organism as dead (protected - only subclasses can kill).
     */
    protected void die() {
        this.isAlive = false;
    }
    
    /**
     * Get current energy level.
     * @return current energy value
     */
    public double getCurrentEnergy() {
        return currentEnergy;
    }
    
    /**
     * Get maximum energy capacity.
     * @return max energy value
     */
    public double getMaxEnergy() {
        return maxEnergy;
    }
    
    /**
     * Get current age in ticks.
     * @return age
     */
    public int getAge() {
        return age;
    }
    
    /**
     * Get organism type name for display/statistics.
     * Implemented by subclasses to return specific type name
     * @return type name
     */
    public abstract String getTypeName();
    
    /**
     * Get energy as percentage (0-100).
     * Implementation from interface.
     * 
     * @return energy percentage
     */
    @Override
    public double getEnergyPercentage() {
        if (maxEnergy == 0) return 0;
        return (currentEnergy / maxEnergy) * 100.0;
    }
    
    /**
     * Consume energy for actions.
     * Subclasses can override for different metabolism rates.
     * 
     * @param amount energy to consume
     */
    protected void consumeEnergy(double amount) {
        currentEnergy = Math.max(0, currentEnergy - amount);
        if (currentEnergy <= 0) {
            die();
        }
    }
    
    /**
     * Add energy from eating.
     * 
     * @param amount energy to add
     */
    protected void addEnergy(double amount) {
        currentEnergy = Math.min(maxEnergy, currentEnergy + amount);
    }
    
    /**
     * Increase age by one tick and check for death by old age.
     */
    protected void ageOneTick() {
        age++;
        if (maxLifespan > 0 && age >= maxLifespan) {
            die();
        }
    }
    
    /**
     * Check if organism is alive and update state (age, hunger, etc.)
     * Default implementation checks age and energy.
     * Subclasses can extend this with additional checks.
     */
    @Override
    public void checkState() {
        ageOneTick();
        if (!isAlive) {
            System.out.println(getTypeName() + " " + id.substring(0, 8) + " died at age " + age);
        }
    }
    
    /**
     * Get species configuration key for probability tables.
     * Implemented by subclasses
     * 
     * @return species key
     */
    public abstract String getSpeciesKey();
}
