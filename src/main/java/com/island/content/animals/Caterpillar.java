package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.Herbivore;
import com.island.content.SpeciesConfig;

/**
 * Caterpillar (Гусінь) - Small insect herbivore.
 * Very small, slow, but high reproduction rate and population capacity.
 * Important food source for many species (ducks, mice, etc.).
 * 
 * Characteristics (from specification):
 * - Weight: 0.01 kg (10 grams)
 * - Max per cell: 1000 (very high!)
 * - Speed: 0 cells/tick (doesn't move!)
 * - Food for saturation: 0 kg (eats constantly, negligible individual consumption)
 * - Lifespan: unlimited (no natural death from age)
 * 
 * Diet: Plants only
 * 
 * GOF Patterns:
 * - Template Method: implements Animal with stationary behavior
 * - Flyweight: shares AnimalType with all other Caterpillar instances
 */
public class Caterpillar extends Animal implements Herbivore {
    
    /**
     * Create a new Caterpillar instance.
     * Uses flyweight AnimalType from SpeciesConfig.
     */
    public Caterpillar() {
        super(SpeciesConfig.getInstance().getAnimalType("caterpillar"));
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
     * Caterpillar eating behavior - continuously eats plants.
     * Since caterpillars don't move, they consume plants in their current cell.
     * 
     * TODO: Implement continuous plant consumption:
     * - Small amount per tick (negligible individually)
     * - Many caterpillars together can defoliate a cell quickly
     */
    @Override
    public double eat() {
        if (!isAlive()) {
            return 0;
        }
        
        // Caterpillars can always try to eat (even with low energy)
        // They don't spend energy on movement or complex actions
        
        // TODO: Consume tiny amount of plant biomass
        /*
        Cell currentCell = getCurrentCell();
        double consumptionRate = 0.001; // Very small per tick
        double consumed = currentCell.consumePlants(consumptionRate, this);
        
        if (consumed > 0) {
            double energyGained = consumed * ENERGY_CONVERSION_RATE;
            addEnergy(energyGained);
            return consumed;
        }
        */
        
        return 0; // Placeholder
    }
    
    /**
     * Caterpillars cannot move (speed = 0).
     * Always returns false.
     */
    @Override
    public boolean move() {
        // Stationary organism
        return false;
    }
    
    /**
     * Caterpillar reproduction - very high rate.
     * Creates offspring when another caterpillar is nearby.
     */
    @Override
    public Caterpillar reproduce() {
        // Caterpillars can reproduce even with low energy
        // They have very high population capacity
        
        // TODO: Find mate and create offspring
        // Offspring appears in same cell (parents don't move)
        
        System.out.println("Caterpillar " + getId().substring(0, 8) + " is reproducing...");
        return null; // Placeholder
    }
}
