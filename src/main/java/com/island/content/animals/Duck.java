package com.island.content.animals;

import com.island.content.Animal;
import com.island.content.Herbivore;
import com.island.content.SpeciesConfig;

public class Duck extends Animal implements Herbivore {

    public Duck() {
        super(
            1.0,       // weight kg
            200,       // max per cell
            4,         // speed (very fast!)
            0.15,      // food for saturation kg
            10000      // max lifespan ticks
        );
    }
    
    @Override
    public String getTypeName() {
        return "Duck";
    }
    
    @Override
    public String getSpeciesKey() {
        return "duck";
    }

    @Override
    public boolean canEat(String preySpeciesKey) {
        // Ducks can eat caterpillars
        if ("caterpillar".equals(preySpeciesKey)) {
            return true;
        }
        // Note: Plants are handled separately in eat() method
        return false;
    }

    @Override
    public int getHuntProbability(String preySpeciesKey) {
        if ("caterpillar".equals(preySpeciesKey)) {
            return 90; // 90% success rate from specification
        }
        return 0;
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
        
        System.out.println("Duck " + id.substring(0, 8) + " is looking for food (caterpillars or plants)...");
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
        
        System.out.println("Duck " + id.substring(0, 8) + " is looking for a mate...");
        return null; // Placeholder
    }
}
