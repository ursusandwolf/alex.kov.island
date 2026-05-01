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
 * Generalized Butterfly using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Butterfly extends SwarmOrganism {
    public Butterfly(long initialBiomass, int speed) {
        super("Butterfly", SpeciesKey.BUTTERFLY, 1000L * SCALE_1M, speed, 30, 
                CATERPILLAR_METABOLISM_RATE_BP, BUTTERFLY_REPRODUCTION_RATE_BP);
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(SimulationNode<Organism> node) {
        long appetite = (getBiomass() * 10) / 100; // 10%
        if (appetite > 0 && node instanceof Cell cell) {
            for (Biomass p : cell.getBiomassContainers()) {
                if (p != this && p.isAlive() && !(p instanceof Caterpillar)) {
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
        if (getBiomass() > 0 && node instanceof Cell cell) {
            long offspringBiomass = (getBiomass() * reproductionRateBP) / SCALE_10K;
            consumeBiomass(offspringBiomass, node);

            Caterpillar c = (Caterpillar) cell.getBiomass(SpeciesKey.CATERPILLAR);
            if (c == null) {
                c = new Caterpillar(0, 0);
                cell.addEntity(c);
            }
            c.spawn(offspringBiomass);
        }
    }
}
