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
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(GrowthComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_LIFECYCLE;
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        // Safe cast due to requiredComponents filter
        Biomass b = (Biomass) entity;
        if (!b.isAlive()) {
            return;
        }
        
        Season season = getEnvironment().getCurrentSeason();
        double growthModifier = season.getGrowthModifier();
        b.grow(cell, growthModifier);
    }
}
