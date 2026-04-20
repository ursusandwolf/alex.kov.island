package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;

/**
 * Rabbit - Herbivore animal implementation.
 * 
 * Characteristics (from specification):
 * - Weight: 2 kg
 * - Max per cell: 150
 * - Speed: 2 cells/tick
 * - Food for saturation: 0.45 kg
 * - Lifespan: 10000 ticks
 * 
 * Diet: Plants only (grass, herbs)
 * 
 * GOF Patterns:
 * - Template Method: implements abstract methods from Animal
 * - Flyweight: shares AnimalType with all other Rabbit instances
 */
public class Rabbit extends Animal {
    
    /**
     * Create a new Rabbit instance.
     * Uses flyweight AnimalType from SpeciesConfig.
     */
    public Rabbit() {
        super(SpeciesConfig.getInstance().getAnimalType("rabbit"));
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
     * Rabbit eating behavior - eats plants.
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        if (!canPerformAction() && !canOnlyEat()) {
            return 0;
        }
        
        System.out.println("Rabbit " + getId().substring(0, 8) + " is looking for grass...");
        return 0; // Placeholder - needs Cell reference to consume plants
    }
    
    /**
     * Rabbit movement - typically random or away from predators.
     */
    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }
        
        return super.move();
    }
    
    /**
     * Rabbit reproduction - rabbits are known for rapid breeding.
     */
    @Override
    public Rabbit reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        System.out.println("Rabbit " + getId().substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder - needs Cell reference to find mate
    }
}
