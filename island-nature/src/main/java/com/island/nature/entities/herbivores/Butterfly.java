package com.island.nature.entities.herbivores;

import com.island.engine.ecs.ComponentRegistry;
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
 * Generalized Butterfly using SwarmOrganism (LOD 1) with integer arithmetic.
 */
public class Butterfly extends SwarmOrganism {
    private final ComponentRegistry registry;

    public Butterfly(Configuration config, ComponentRegistry registry, SpeciesKey key, long initialBiomass, long maxBiomass, int speed) {
        super(config, registry, "Butterfly", key, maxBiomass, speed, 30, 
                config.getCaterpillarMetabolismRateBP(), config.getButterflyReproductionRateBP());
        this.registry = registry;
        spawn(initialBiomass);
    }

    @Override
    protected void processFeeding(Cell cell) {
        long appetite = (getBiomass() * 10) / 100; // 10%
        if (appetite > 0) {
            for (Biomass p : cell.getBiomassContainers()) {
                if (p != this && p.isAlive() && !p.getSpeciesKey().equals(getSpeciesKey())) {
                    long consumed = (appetite * config.getScale10K()) / config.getCaterpillarFeedEfficiencyBP();
                    long actualEaten = p.consumeBiomass(consumed, cell);
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
    protected void processReproduction(Cell cell) {
        if (getBiomass() > 0) {
            long offspringBiomass = (getBiomass() * reproductionRateBP) / config.getScale10K();
            consumeBiomass(offspringBiomass, cell);

            NatureWorld nw = (NatureWorld) cell.getWorld();
            SpeciesKey catKey = nw.getRegistry().getKey("caterpillar").orElse(null);
            if (catKey != null) {
                Caterpillar c = (Caterpillar) cell.getBiomass(catKey);
                if (c == null) {
                    AnimalType type = nw.getRegistry().getBiomassType(catKey).orElseThrow();
                    long capacity = type.getWeight() * type.getMaxPerCell();
                    c = new Caterpillar(config, registry, catKey, 0, capacity, type.getSpeed());
                    cell.addEntity(c);
                }
                c.spawn(offspringBiomass);
            }
        }
    }
}