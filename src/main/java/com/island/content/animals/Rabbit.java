package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.SpeciesConfig;
import com.island.content.Herbivore;

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
public class Rabbit extends Animal implements Herbivore {
    
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
        // Rabbits only eat plants, not animals
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
     * TODO: Implement plant consumption logic:
     * 1. Get total plant biomass in current cell
     * 2. Calculate how much rabbit needs to reach saturation
     * 3. Consume biomass from plants
     * 4. Convert biomass to energy
     * 
     * Note: Multiple rabbits can eat from same plants simultaneously
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        // Can eat even with low energy (canOnlyEat or canPerformAction)
        if (!canPerformAction() && !canOnlyEat()) {
            return 0;
        }
        
        // TODO: Implement plant eating logic
        // Pseudocode:
        // Cell currentCell = getCurrentCell();
        // double needed = getFoodForSaturation() - (getCurrentEnergy() / getMaxEnergy() * getFoodForSaturation());
        // double available = currentCell.getTotalPlantBiomass();
        // double consumed = Math.min(needed, available);
        // if (consumed > 0) {
        //     // Remove biomass from plants proportionally
        //     double energyGained = consumed * energyConversionRate;
        //     addEnergy(energyGained);
        //     return energyGained;
        // }
        
        System.out.println("Rabbit " + id.substring(0, 8) + " is looking for grass...");
        return 0; // Placeholder
    }
    
    /**
     * Rabbit movement - typically random or away from predators.
     * TODO: Implement predator avoidance behavior:
     * - Detect predators in adjacent cells
     * - Move in opposite direction
     */
    @Override
    public boolean move() {
        if (!canPerformAction()) {
            return false;
        }
        
        // TODO: Implement rabbit-specific movement
        // Options:
        // 1. Random movement (simplest)
        // 2. Flee from predators (advanced)
        // 3. Move toward high plant density (advanced)
        
        return super.move();
    }
    
    /**
     * Rabbit reproduction - rabbits are known for rapid breeding.
     * Creates new rabbit offspring when another rabbit is present.
     * 
     * TODO: Complete implementation similar to Wolf.reproduce()
     */
    @Override
    public Rabbit reproduce() {
        if (!canPerformAction()) {
            return null;
        }
        
        // TODO: Find mate and create offspring
        
        System.out.println("Rabbit " + id.substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder
    }
}
