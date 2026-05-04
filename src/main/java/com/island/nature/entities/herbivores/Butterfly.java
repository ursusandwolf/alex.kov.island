package com.island.nature.entities.herbivores;

import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SwarmOrganism;
import com.island.engine.SimulationNode;
import com.island.nature.model.Cell;

/**
 * Generalized Butterfly using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Butterfly extends SwarmOrganism {
    public Butterfly(Configuration config, long initialBiomass, long maxBiomass, int speed) {
        super(config, "Butterfly", SpeciesKey.BUTTERFLY, maxBiomass, speed, 30, 
                config.getCaterpillarMetabolismRateBP(), config.getButterflyReproductionRateBP());
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(SimulationNode<Organism> node) {
        long appetite = (getBiomass() * 10) / 100; // 10%
        if (appetite > 0 && node instanceof Cell cell) {
            for (Biomass p : cell.getBiomassContainers()) {
                if (p != this && p.isAlive() && !(p instanceof Caterpillar)) {
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
        if (getBiomass() > 0 && node instanceof Cell cell) {
            long offspringBiomass = (getBiomass() * reproductionRateBP) / config.getScale10K();
            consumeBiomass(offspringBiomass, node);

            Caterpillar c = (Caterpillar) cell.getBiomass(SpeciesKey.CATERPILLAR);
            if (c == null) {
                NatureWorld nw = (NatureWorld) cell.getWorld();
                AnimalType type = nw.getRegistry().getBiomassType(SpeciesKey.CATERPILLAR).orElseThrow();
                long capacity = type.getWeight() * type.getMaxPerCell();
                c = new Caterpillar(config, 0, capacity, type.getSpeed());
                cell.addEntity(c);
            }
            c.spawn(offspringBiomass);
        }
    }
}
