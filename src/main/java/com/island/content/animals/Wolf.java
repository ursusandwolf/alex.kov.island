package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;
import com.island.content.Predator;

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
 */
public class Wolf extends Animal implements Predator {
    
    /**
     * Create a new Wolf instance.
     */
    public Wolf() {
        super(
            50.0,      // weight kg
            30,        // max per cell
            3,         // speed
            8.0,       // food for saturation kg
            10000      // max lifespan ticks
        );
    }
    
    @Override
    public String getTypeName() {
        return "Wolf";
    }
    
    @Override
    public String getSpeciesKey() {
        return "wolf";
    }
    
    /**
     * Check if wolf can eat specific prey.
     * Delegates to SpeciesConfig for probability data.
     */
    @Override
    public boolean canEat(String preySpeciesKey) {
        return SpeciesConfig.getInstance().canEat("wolf", preySpeciesKey);
    }
    
    /**
     * Get hunting success probability.
     */
    @Override
    public int getHuntProbability(String preySpeciesKey) {
        return SpeciesConfig.getInstance().getHuntProbability("wolf", preySpeciesKey);
    }
    
    /**
     * Wolf eating behavior.
     * TODO: Implement complete hunting logic:
     * 1. Get list of potential prey in current cell
     * 2. Sort by priority (highest probability first)
     * 3. Attempt hunt using probability roll
     * 4. If successful, consume prey and gain energy
     * 5. Prey is marked as eaten (removed from simulation)
     * 
     * Note: Plants are not part of wolf's diet
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        // TODO: Implement hunting logic
        // Pseudocode:
        // Cell currentCell = getCurrentCell(); // Need reference to cell
        // for (Animal prey : currentCell.getAnimals()) {
        //     if (canEat(prey.getSpeciesKey())) {
        //         if (SpeciesConfig.getInstance().rollHuntSuccess("wolf", prey.getSpeciesKey())) {
        //             // Successful hunt
        //             prey.die();
        //             double energyGained = prey.getWeight() * energyConversionRate;
        //             addEnergy(energyGained);
        //             return energyGained;
        //         }
        //     }
        // }
        
        System.out.println("Wolf " + id.substring(0, 8) + " is looking for prey...");
        return 0; // Placeholder
    }
    
    /**
     * Wolf movement behavior.
     * TODO: Implement pack hunting behavior (optional advanced feature):
     * - Wolves may coordinate movement with other wolves in nearby cells
     * - Move toward cells with high prey density
     */
    @Override
    public boolean move() {
        // Call parent implementation for energy check
        if (!canPerformAction()) {
            return false;
        }
        
        // TODO: Implement wolf-specific movement strategy
        // Options:
        // 1. Random movement (simplest)
        // 2. Move toward prey scent (advanced)
        // 3. Pack coordination (very advanced)
        
        return super.move();
    }
    
    /**
     * Wolf reproduction.
     * Creates new wolf offspring when conditions are met.
     * 
     * TODO: Complete implementation:
     * 1. Find another wolf in same cell
     * 2. Create offspring
     * 3. Split energy: parent keeps 50%, offspring gets 50%
     * 4. Add offspring to cell
     */
    @Override
    public Wolf reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        // TODO: Find mate and create offspring
        // Wolf offspring = new Wolf();
        // double sharedEnergy = getCurrentEnergy() / 2;
        // currentEnergy = sharedEnergy;
        // offspring.addEnergy(sharedEnergy);
        // return offspring;
        
        System.out.println("Wolf " + id.substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder
    }
}
