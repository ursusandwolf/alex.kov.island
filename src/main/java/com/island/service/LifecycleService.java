package com.island.service;

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
public class LifecycleService extends AbstractService<Cell> {

    public LifecycleService(SimulationWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    protected void processCell(Cell cell, int tickCount) {
        processAging(cell);
        processBiomassGrowth(cell);
    }

    private void processAging(Cell cell) {
        cell.forEachAnimal(a -> {
            if (a.isAlive()) {
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

    private void processBiomassGrowth(Cell cell) {
        for (Biomass b : cell.getBiomassContainers()) {
            if (b.isAlive()) {
                b.grow(cell);
            }
        }
    }
}
