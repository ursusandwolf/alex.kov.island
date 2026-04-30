package com.island.service;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for aging, energy decay, and natural growth/death using integer arithmetic.
 */
public class LifecycleService extends AbstractService<SimulationNode> {

    public LifecycleService(SimulationWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    protected void processCell(SimulationNode node, int tickCount) {
        processAging(node);
        processBiomassGrowth(node);
    }

    private void processAging(SimulationNode node) {
        node.getLivingEntities().forEach(m -> {
            if (m instanceof Animal a && a.isAlive()) {
                // 1. Metabolism (Energy decay)
                long metabolism = a.getDynamicMetabolismRate();
                
                // Endangered protection: reduce metabolism if protected
                if (protectionMap != null && protectionMap.containsKey(a.getSpeciesKey())) {
                    metabolism = (metabolism * (com.island.config.SimulationConstants.SCALE_10K - 5000)) / com.island.config.SimulationConstants.SCALE_10K;
                }

                if (!a.tryConsumeEnergy(metabolism)) {
                    getWorld().reportDeath(a.getSpeciesKey(), DeathCause.HUNGER);
                }
                
                // 2. Age increment and death check
                if (a.isAlive() && a.checkAgeDeath()) {
                    getWorld().reportDeath(a.getSpeciesKey(), DeathCause.AGE);
                }
            }
        });
    }

    private void processBiomassGrowth(SimulationNode node) {
        for (com.island.engine.Mortal m : node.getBiomassEntities()) {
            if (m instanceof Biomass b && b.isAlive()) {
                b.grow(node);
            }
        }
    }
}
