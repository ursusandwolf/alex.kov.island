package com.island.content.animals.herbivores;

import com.island.content.plants.Plant;
import com.island.content.SpeciesConfig;

/**
 * Optimized Caterpillar: Now acts as a biomass container (like plants).
 * This eliminates millions of individual objects from the simulation.
 */
public class Caterpillar extends Plant {
    public Caterpillar() {
        super("Caterpillar", "caterpillar",
              SpeciesConfig.getInstance().getAnimalType("caterpillar").getWeight() * 
              SpeciesConfig.getInstance().getAnimalType("caterpillar").getMaxPerCell());
    }

    /**
     * The Pendulum Logic: 
     * 1. Lose mass (natural decay/metabolism).
     * 2. Consume other plants in the cell to gain mass.
     */
    public void processPendulum(com.island.model.Cell cell) {
        // 1. Natural Decay (Metabolism)
        double decayAmount = biomass * com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;
        biomass -= decayAmount;
        
        // 2. Return nutrients to Grass (Fertilizer)
        for (Plant p : cell.getPlants()) {
            if (p instanceof com.island.content.plants.Grass) {
                p.addBiomass(decayAmount * com.island.config.SimulationConstants.CATERPILLAR_FERTILIZER_EFFICIENCY);
                break;
            }
        }

        // 3. Consume Plants to grow
        double appetite = maxBiomass * 0.15; // Target growth effort
        for (Plant p : cell.getPlants()) {
            if (p != this && !(p instanceof Caterpillar)) { 
                double eaten = p.consumeBiomass(appetite);
                // Apply conversion efficiency
                biomass = Math.min(maxBiomass, biomass + (eaten * com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY));
                appetite -= eaten;
                if (appetite <= 0) break;
            }
        }
    }

    @Override
    public void grow() {
        // Growth is now handled via processPendulum(Cell)
    }
}
