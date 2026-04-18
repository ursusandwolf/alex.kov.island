package com.island.content;

/**
 * Interface defining basic life cycle behaviors for all organisms.
 * Uses Template Method pattern - specific implementations will define the details.
 * 
 * GRASP: Information Expert - organism knows how to perform its own actions
 */
public interface OrganismBehavior {
    
    /**
     * Attempt to eat available food in the current cell.
     * @return amount of energy gained from eating (0 if failed)
     */
    double eat();
    
    /**
     * Move to a neighboring cell if possible.
     * @return true if movement was successful, false otherwise
     */
    boolean move();
    
    /**
     * Attempt to reproduce if conditions are met.
     * @return new organism instance if reproduction succeeded, null otherwise
     */
    Organism reproduce();
    
    /**
     * Check if organism is alive and update state (age, hunger, etc.)
     */
    void checkState();
    
    /**
     * Get current energy level as percentage (0-100).
     * @return energy percentage
     */
    double getEnergyPercentage();
    
    /**
     * Check if organism can perform active actions (requires min 30% energy).
     * @return true if energy >= 30%
     */
    default boolean canPerformAction() {
        return getEnergyPercentage() >= 30.0;
    }
    
    /**
     * Check if organism can only eat (energy between 0 and 30%).
     * @return true if 0 < energy < 30%
     */
    default boolean canOnlyEat() {
        double energy = getEnergyPercentage();
        return energy > 0 && energy < 30.0;
    }
}
