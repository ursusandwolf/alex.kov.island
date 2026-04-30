package com.island.service;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for aging, energy decay, and natural growth/death.
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
        List<Animal> animals = new ArrayList<>(cell.getAnimals());
        for (Animal a : animals) {
            if (a.isAlive()) {
                // 1. Metabolism
                double metabolism = a.getDynamicMetabolismRate();
                if (!a.tryConsumeEnergy(metabolism)) {
                    getWorld().reportDeath(a.getSpeciesKey(), DeathCause.HUNGER);
                }
                
                // 2. Age
                if (a.isAlive() && a.checkAgeDeath()) {
                    getWorld().reportDeath(a.getSpeciesKey(), DeathCause.AGE);
                }
            }
        }
    }

    private void processBiomassGrowth(Cell cell) {
        for (Biomass b : cell.getBiomassContainers()) {
            if (b.isAlive()) {
                b.grow(cell);
            }
        }
    }
}
