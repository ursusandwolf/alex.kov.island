package com.island.nature.entities.herbivores;

import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.engine.core.SimulationNode;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.core.SwarmOrganism;
import com.island.nature.entities.domain.NatureWorld;

/**
 * Generalized Caterpillar using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Caterpillar extends SwarmOrganism {
    public Caterpillar(Configuration config, SpeciesKey key, long initialBiomass, long maxBiomass, int speed) {
        super(config, "Caterpillar", key, maxBiomass, speed, 30,
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

            NatureWorld nw = (NatureWorld) cell.getWorld();
            SpeciesKey flyKey = nw.getRegistry().getKey("butterfly").orElse(null);
            if (flyKey != null) {
                Butterfly b = (Butterfly) cell.getBiomass(flyKey);
                if (b == null) {
                    AnimalType type = nw.getRegistry().getBiomassType(flyKey).orElseThrow();
                    long capacity = type.getWeight() * type.getMaxPerCell();
                    b = new Butterfly(config, flyKey, 0, capacity, type.getSpeed());
                    cell.addEntity(b);
                }
                b.spawn(readyToTransform);
            }
        }
    }
}