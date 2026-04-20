package com.island.content;

/**
 * Abstract class for all animals (both predators and herbivores).
 * Extends Organism with animal-specific properties.
 * 
 * GOF Patterns:
 * - Template Method: eat(), move(), reproduce() have default implementations
 * - Flyweight: shares AnimalType instance across all animals of same species
 * 
 * GRASP Principles:
 * - Creator: Animals create other animals during reproduction
 * - Low Coupling: Depends on Cell interface, not concrete implementation
 */
public abstract class Animal extends Organism {
    
    // Flyweight reference - shared across all instances of this species
    protected final AnimalType animalType;
    
    /**
     * Constructor for Animal using Flyweight pattern.
     * All intrinsic state is stored in the shared AnimalType.
     * 
     * @param animalType the flyweight containing species-specific data
     */
    protected Animal(AnimalType animalType) {
        super(animalType.getMaxEnergy(), animalType.getMaxLifespan());
        this.animalType = animalType;
    }
    
    /**
     * Get animal weight from flyweight.
     * @return weight in kg
     */
    public double getWeight() {
        return animalType.getWeight();
    }
    
    /**
     * Get maximum individuals per cell from flyweight.
     * @return max count
     */
    public int getMaxPerCell() {
        return animalType.getMaxPerCell();
    }
    
    /**
     * Get movement speed from flyweight.
     * @return cells per tick
     */
    public int getSpeed() {
        return animalType.getSpeed();
    }
    
    /**
     * Get food needed for saturation from flyweight.
     * @return kg of food
     */
    public double getFoodForSaturation() {
        return animalType.getFoodForSaturation();
    }
    
    /**
     * Get the flyweight AnimalType for this species.
     * @return shared AnimalType instance
     */
    public AnimalType getAnimalType() {
        return animalType;
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
     * Delegates to flyweight AnimalType.
     * 
     * @param preySpeciesKey the species key of potential prey
     * @return true if this animal can eat the prey
     */
    public boolean canEat(String preySpeciesKey) {
        return animalType.canEat(preySpeciesKey);
    }
    
    /**
     * Get hunting success probability for a prey species.
     * Delegates to flyweight AnimalType.
     * 
     * @param preySpeciesKey the species key of potential prey
     * @return probability percentage (0-100)
     */
    public int getHuntProbability(String preySpeciesKey) {
        return animalType.getHuntProbability(preySpeciesKey);
    }
}
