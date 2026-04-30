package com.island.content.animals.herbivores;

import static com.island.config.SimulationConstants.BUTTERFLY_REPRODUCTION_RATE_BP;
import static com.island.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY_BP;
import static com.island.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE_BP;
import static com.island.config.SimulationConstants.SCALE_10K;
import static com.island.config.SimulationConstants.SCALE_1M;

import com.island.content.Biomass;
import com.island.content.SpeciesKey;
import com.island.content.SwarmOrganism;
import com.island.engine.SimulationNode;
import com.island.model.Cell;
import java.util.List;

/**
 * Generalized Caterpillar using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Caterpillar extends SwarmOrganism {
    public Caterpillar(long initialBiomass, int speed) {
        super("Caterpillar", SpeciesKey.CATERPILLAR, 1000L * SCALE_1M, speed, 30, 
                CATERPILLAR_METABOLISM_RATE_BP, BUTTERFLY_REPRODUCTION_RATE_BP);
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(SimulationNode node) {
        long appetite = (biomass * 10) / 100; // 10%
        if (appetite > 0) {
            List<? extends com.island.engine.Mortal> availablePlants = node.getBiomassEntities();
            for (com.island.engine.Mortal m : availablePlants) {
                if (m instanceof Biomass p && p != this && p.isAlive() && !(p instanceof Butterfly)) {
                    long consumed = (appetite * SCALE_10K) / CATERPILLAR_FEED_EFFICIENCY_BP;
                    long actualEaten = p.consumeBiomass(consumed, node);
                    long energyGain = (actualEaten * CATERPILLAR_FEED_EFFICIENCY_BP) / SCALE_10K;
                    spawn(energyGain);
                    appetite -= energyGain;
                    if (appetite <= 0) {
                        break;
                    }
                }
            }
        }
    }

    @Override
    protected void processReproduction(SimulationNode node) {
        // Reproduce if old enough (max age bucket)
        long readyToTransform = ageBuckets[ageBuckets.length - 1];
        if (readyToTransform > 0) {
            ageBuckets[ageBuckets.length - 1] = 0;
            updateTotalBiomass();

            if (node instanceof Cell cell) {
                Butterfly b = (Butterfly) cell.getBiomass(SpeciesKey.BUTTERFLY);
                if (b == null) {
                    b = new Butterfly(0, 0);
                    cell.addBiomass(b);
                }
                b.spawn(readyToTransform);
            }
        }
    }
}
