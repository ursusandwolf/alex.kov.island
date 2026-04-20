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
 */
public class Rabbit extends Animal {
    
    /**
     * Create a new Rabbit instance.
     */
    public Rabbit() {
        super(
            2.0,       // weight kg
            150,       // max per cell
            2,         // speed
            0.45,      // food for saturation kg
            10000      // max lifespan ticks
        );
    }
    
    @Override
    public String getTypeName() {
        return "Rabbit";
    }
    
    @Override
    public String getSpeciesKey() {
        return "rabbit";
    }
    
    /**
     * Rabbits cannot eat other animals.
     */
    @Override
    public boolean canEat(String preySpeciesKey) {
        return false;
    }
    
    /**
     * Rabbits don't hunt - always return 0.
     */
    @Override
    public int getHuntProbability(String preySpeciesKey) {
        return 0;
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
