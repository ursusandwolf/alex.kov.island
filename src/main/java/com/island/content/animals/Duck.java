package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.Herbivore;
import com.island.content.SpeciesConfig;

/**
 * Duck - Special herbivore that can eat both plants AND insects (caterpillars).
 * This demonstrates complex diet behavior where an herbivore can also consume small animals.
 * 
 * Characteristics (from specification):
 * - Weight: 1 kg
 * - Max per cell: 200
 * - Speed: 4 cells/tick (fast!)
 * - Food for saturation: 0.15 kg
 * - Lifespan: 10000 ticks
 * 
 * Diet: 
 * - Primary: Plants
 * - Secondary: Caterpillars (90% success rate)
 * 
 * GOF Patterns:
 * - Strategy: Uses different eating strategies for plants vs insects
 * - Flyweight: shares AnimalType with all other Duck instances
 */
public class Duck extends Animal implements Herbivore {
    
    /**
     * Create a new Duck instance.
     * Uses flyweight AnimalType from SpeciesConfig.
     */
    public Duck() {
        super(SpeciesConfig.getInstance().getAnimalType("duck"));
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
     * Duck eating behavior - dual diet strategy.
     * TODO: Implement two-phase eating:
     * Phase 1: Try to eat caterpillars first (higher priority, easier to catch)
     * Phase 2: If still hungry or no caterpillars, eat plants
     * 
     * Important: Duck moves first (speed 4), so it gets first access to caterpillars
     * before slower predators might compete for them.
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        if (!canPerformAction() && !canOnlyEat()) {
            return 0;
        }
        
        double totalEaten = 0;
        
        // TODO: Phase 1 - Hunt caterpillars
        /*
        Cell currentCell = getCurrentCell();
        for (Animal prey : currentCell.getAnimals()) {
            if ("caterpillar".equals(prey.getSpeciesKey())) {
                if (SpeciesConfig.getInstance().rollHuntSuccess("duck", "caterpillar")) {
                    prey.die();
                    double energyGained = prey.getWeight() * ENERGY_CONVERSION_RATE;
                    addEnergy(energyGained);
                    totalEaten += prey.getWeight();
                    
                    // Check if satisfied
                    if (totalEaten >= getFoodForSaturation()) {
                        return totalEaten;
                    }
                }
            }
        }
        */
        
        // TODO: Phase 2 - Eat plants if still hungry
        /*
        if (totalEaten < getFoodForSaturation()) {
            double plantNeeded = getFoodForSaturation() - totalEaten;
            double plantConsumed = currentCell.consumePlants(plantNeeded, this);
            totalEaten += plantConsumed;
            
            double energyFromPlants = plantConsumed * ENERGY_CONVERSION_RATE;
            addEnergy(energyFromPlants);
        }
        */
        
        System.out.println("Duck " + getId().substring(0, 8) + " is looking for food (caterpillars or plants)...");
        return totalEaten; // Placeholder
    }
    
    /**
     * Duck movement - very fast (4 cells/tick).
     * TODO: Implement fast movement with predator avoidance.
     */
    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }
        
        // TODO: Use high speed to flee from predators or find food
        // Duck can move up to 4 cells in one turn
        
        return super.move();
    }
    
    /**
     * Duck reproduction.
     */
    @Override
    public Duck reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        // TODO: Find mate and create offspring
        
        System.out.println("Duck " + getId().substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder
    }
}
