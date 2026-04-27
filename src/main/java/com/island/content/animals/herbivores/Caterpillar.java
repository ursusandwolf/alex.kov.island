package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_FERTILIZER_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;

import com.island.content.SpeciesConfig;
import com.island.content.SpeciesKey;
import com.island.content.plants.Plant;
import com.island.model.Cell;
import java.util.List;

/**
 * Optimized Caterpillar: Now acts as a biomass container (like plants).
 * This eliminates millions of individual objects from the simulation.
 */
public class Caterpillar extends Plant {
    public Caterpillar() {
        super("Caterpillar", SpeciesKey.CATERPILLAR,
                SpeciesConfig.getInstance().getAnimalType(SpeciesKey.CATERPILLAR).getWeight() 
                * SpeciesConfig.getInstance().getAnimalType(SpeciesKey.CATERPILLAR).getMaxPerCell());
    }

    /**
     * The Pendulum Logic: 
     * 1. Lose mass (natural decay/metabolism).
     * 2. Consume other plants (Grass, Cabbage).
     * 3. Mass increases (breeding/growth).
     */
    public void processPendulum(Cell cell) {
        // 1. Natural metabolic loss
        double metabolicLoss = biomass * CATERPILLAR_METABOLISM_RATE;
        biomass -= metabolicLoss;

        // 2. Feed on actual plants in the same cell
        double appetite = maxBiomass * 0.10; 
        List<Plant> availablePlants = cell.getPlants();
        for (Plant p : availablePlants) {
            if (p != this && p.isAlive()) {
                double eaten = p.consumeBiomass(appetite * (1.0 / CATERPILLAR_FEED_EFFICIENCY));
                biomass = Math.min(maxBiomass, biomass + (eaten * CATERPILLAR_FEED_EFFICIENCY));
                appetite -= eaten;
                if (appetite <= 0) {
                    break;
                }
            }
        }
    }

    @Override
    public void grow() {
        // Growth is now handled via processPendulum(Cell)
    }
}
