package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.Predator;
import com.island.content.SpeciesConfig;

/**
 * Fox - Predator animal implementation.
 * 
 * Characteristics:
 * - Weight: 8 kg
 * - Max per cell: 30
 * - Speed: 2 cells/tick
 * - Food for saturation: 2 kg
 * - Lifespan: 10000 ticks
 * 
 * Diet: Hunts small animals (rabbits, mice, ducks, caterpillars)
 * 
 * GOF Patterns:
 * - Template Method: implements abstract methods from Animal
 * - Flyweight: shares AnimalType with all other Fox instances
 */
public class Fox extends Animal implements Predator {
    
    /**
     * Create a new Fox instance.
     * Uses flyweight AnimalType from SpeciesConfig.
     */
    public Fox() {
        super(SpeciesConfig.getInstance().getAnimalType("fox"));
    }

    @Override
    public String getTypeName() {
        return animalType.getTypeName();
    }

    @Override
    public String getSpeciesKey() {
        return animalType.getSpeciesKey();
    }
    
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }

        System.out.println("Fox " + getId().substring(0, 8) + " is looking for prey...");
        return 0; // Placeholder - needs Cell reference to implement hunting
    }

    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }

        return super.move();
    }

    @Override
    public Fox reproduce() {
        if (!canPerformAction()) {
            return null;
        }

        System.out.println("Fox " + getId().substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder - needs Cell reference to find mate
    }
}
