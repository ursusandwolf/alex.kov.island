package com.island.nature.service;

import com.island.engine.ecs.Component;
import com.island.nature.entities.components.GrowthComponent;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.environment.Season;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for biomass growth.
 */
public class BiomassGrowthSystem extends NatureEntitySystem {

    public BiomassGrowthSystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of();
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of(GrowthComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_LIFECYCLE;
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        // Safe cast due to query filter
        Biomass b = (Biomass) entity;
        if (b.getBiomass() <= 0) {
            return;
        }
        
        Season season = getEnvironment().getCurrentSeason();
        double growthModifier = season.getGrowthModifier();
        
        // Temperature-based growth factor
        int temp = getEnvironment().getTemperature();
        double tempFactor = 1.0;
        if (temp < 0) {
            tempFactor = 0.2; // Frozen
        } else if (temp < 10) {
            tempFactor = 0.5; // Chilly
        } else if (temp > 35) {
            tempFactor = 0.4; // Heat stress
        }
        
        long old = b.getBiomass();
        long growth = (b.getMaxBiomass() * config.getPlantGrowthRateBP()) / config.getScale10K();
        growth = (long) (growth * growthModifier * tempFactor);
        b.setBiomass(Math.min(b.getMaxBiomass(), b.getBiomass() + growth));
        
        long delta = b.getBiomass() - old;
        if (delta != 0) {
            getWorld().getStatisticsService().registerBiomassChange(b.getSpeciesKey(), delta);
        }
    }
}
