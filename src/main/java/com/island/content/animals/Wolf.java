package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;
import com.island.content.AnimalType;

/**
 * Wolf - Predator animal implementation.
 * 
 * Characteristics (from specification):
 * - Weight: 50 kg
 * - Max per cell: 30
 * - Speed: 3 cells/tick
 * - Food for saturation: 8 kg
 * - Lifespan: 10000 ticks
 * 
 * Diet: Hunts herbivores with varying success rates
 * 
 * GOF Patterns:
 * - Template Method: implements abstract methods from Animal
 * - Flyweight: shares AnimalType with all other Wolf instances
 */
public class Wolf extends Animal {
    
    /**
     * Create a new Wolf instance.
     * Uses flyweight AnimalType from SpeciesConfig.
     */
    public Wolf() {
        super(SpeciesConfig.getInstance().getAnimalType("wolf"));
    }
    
    @Override
    public String getTypeName() {
        return animalType.getTypeName();
    }
    
    @Override
    public String getSpeciesKey() {
        return animalType.getSpeciesKey();
    }
    
    /**
     * Wolf eating behavior - hunts other animals.
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        System.out.println("Wolf " + getId().substring(0, 8) + " is looking for prey...");
        return 0; // Placeholder - needs Cell reference to implement hunting
    }
    
    /**
     * Wolf movement behavior.
     */
    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }
        
        return super.move();
    }
    
    /**
     * Wolf reproduction.
     * Creates new wolf offspring when conditions are met.
     */
    @Override
    public Wolf reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        System.out.println("Wolf " + getId().substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder - needs Cell reference to find mate
    }
}
