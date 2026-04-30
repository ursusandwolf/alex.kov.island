package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.BUTTERFLY_REPRODUCTION_RATE;
import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.content.SwarmOrganism;
import com.island.model.Cell;
import java.util.List;

/**
 * Generalized Butterfly using SwarmOrganism (LOD 1).
 */
public class Butterfly extends SwarmOrganism {
    public Butterfly(double initialBiomass, int speed) {
        super("Butterfly", SpeciesKey.BUTTERFLY, 1000, speed, 30, 
                CATERPILLAR_METABOLISM_RATE, BUTTERFLY_REPRODUCTION_RATE);
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(Cell cell) {
        double appetite = biomass * 0.10; 
        if (appetite > 0) {
            List<Biomass> availablePlants = cell.getBiomassContainers();
            for (Biomass p : availablePlants) {
                if (p != this && p.isAlive() && !(p instanceof Caterpillar)) {
                    double eaten = p.consumeBiomass(appetite / CATERPILLAR_FEED_EFFICIENCY, cell);
                    spawn(eaten * CATERPILLAR_FEED_EFFICIENCY);
                    appetite -= eaten;
                    if (appetite <= 0) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void processReproduction(Cell cell) {
        if (biomass > 0) {
            double offspringBiomass = biomass * reproductionRate;
            consumeBiomass(offspringBiomass, cell);

            Caterpillar c = (Caterpillar) cell.getBiomass(SpeciesKey.CATERPILLAR);
            if (c == null) {
                c = new Caterpillar(0, 0);
                cell.addBiomass(c);
            }
            c.spawn(offspringBiomass);
        }
    }
}
