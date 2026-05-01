package com.island.nature.entities.herbivores;

import static com.island.nature.config.SimulationConstants.BUTTERFLY_REPRODUCTION_RATE_BP;
import static com.island.nature.config.SimulationConstants.CATERPILLAR_FEED_EFFICIENCY_BP;
import static com.island.nature.config.SimulationConstants.CATERPILLAR_METABOLISM_RATE_BP;
import static com.island.nature.config.SimulationConstants.SCALE_10K;
import static com.island.nature.config.SimulationConstants.SCALE_1M;

import com.island.nature.entities.Biomass;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SwarmOrganism;
import com.island.engine.SimulationNode;
import com.island.nature.model.Cell;

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
    protected void processFeeding(SimulationNode<Organism> node) {
        long appetite = (getBiomass() * 10) / 100; // 10%
        if (appetite > 0 && node instanceof Cell cell) {
            for (Biomass p : cell.getBiomassContainers()) {
                if (p != this && p.isAlive() && !(p instanceof Butterfly)) {
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
    protected void processReproduction(SimulationNode<Organism> node) {
        // Reproduce if old enough (max age bucket)
        long readyToTransform = ageBuckets[ageBuckets.length - 1];
        if (readyToTransform > 0 && node instanceof Cell cell) {
            ageBuckets[ageBuckets.length - 1] = 0;
            updateTotalBiomass();

            Butterfly b = (Butterfly) cell.getBiomass(SpeciesKey.BUTTERFLY);
            if (b == null) {
                b = new Butterfly(0, 0);
                cell.addEntity(b);
            }
            b.spawn(readyToTransform);
        }
    }
}
