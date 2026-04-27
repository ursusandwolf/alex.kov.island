package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.BUTTERFLY_REPRODUCTION_RATE;
import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.model.Cell;
import java.util.List;

/**
 * Butterfly implementation (Biomass container).
 * Butterflies now live "long" like caterpillars, with metabolism and feeding.
 */
public class Butterfly extends Biomass {
    public Butterfly(double initialBiomass, int speed) {
        super("Butterfly", SpeciesKey.BUTTERFLY, initialBiomass, speed);
    }

    public void processPendulum(Cell cell) {
        // 1. Natural metabolic loss
        double metabolicLoss = biomass * CATERPILLAR_METABOLISM_RATE;
        biomass = Math.max(0, biomass - metabolicLoss);

        // 2. Feed on actual plants (Butterflies also eat to survive)
        double appetite = biomass * 0.10; 
        if (appetite > 0) {
            List<Biomass> availablePlants = cell.getBiomassContainers();
            for (Biomass p : availablePlants) {
                if (p != this && p.isAlive() && !(p instanceof Caterpillar)) {
                    double eaten = p.consumeBiomass(appetite * (1.0 / CATERPILLAR_FEED_EFFICIENCY));
                    biomass += (eaten * CATERPILLAR_FEED_EFFICIENCY);
                    appetite -= eaten;
                    if (appetite <= 0) {
                        break;
                    }
                }
            }
        }

        // 3. Reproduction (lay eggs)
        reproduce(cell);
    }

    private void reproduce(Cell cell) {
        if (biomass > 0) {
            double offspringBiomass = biomass * BUTTERFLY_REPRODUCTION_RATE;
            biomass -= offspringBiomass; // Conversion of mass

            Caterpillar c = (Caterpillar) cell.getBiomass(SpeciesKey.CATERPILLAR);
            if (c == null) {
                // If no caterpillars exist, create a new container
                c = new Caterpillar(0, 0); // Speed 0 for caterpillars
                cell.addBiomass(c);
            }
            c.spawn(offspringBiomass);
        }
    }

    public void addBiomass(double amount) {
        this.biomass += amount;
    }

    @Override
    public void grow() {
        // Growth is handled via processPendulum and metamorphosis
    }
}
