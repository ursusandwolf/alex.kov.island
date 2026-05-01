package com.island.nature.entities.herbivores;

import com.island.nature.config.Configuration;
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
    public Caterpillar(Configuration config, long initialBiomass, int speed) {
        super(config, "Caterpillar", SpeciesKey.CATERPILLAR, 1000L * config.getScale1M(), speed, 30,
                config.getCaterpillarMetabolismRateBP(), config.getButterflyReproductionRateBP());
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(SimulationNode<Organism> node) {
        long appetite = (getBiomass() * 10) / 100; // 10%
        if (appetite > 0 && node instanceof Cell cell) {
            for (Biomass p : cell.getBiomassContainers()) {
                if (p != this && p.isAlive() && !(p instanceof Butterfly)) {
                    long consumed = (appetite * config.getScale10K()) / config.getCaterpillarFeedEfficiencyBP();
                    long actualEaten = p.consumeBiomass(consumed, node);
                    long energyGain = (actualEaten * config.getCaterpillarFeedEfficiencyBP()) / config.getScale10K();
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
                b = new Butterfly(config, 0, 0);
                cell.addEntity(b);
            }
            b.spawn(readyToTransform);
        }
    }
}
