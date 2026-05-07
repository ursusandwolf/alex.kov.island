package com.island.nature.service;

import com.island.engine.ecs.Component;
import com.island.nature.entities.components.AgeComponent;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.components.MetabolismComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.environment.Season;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for animal aging and metabolism.
 */
public class AnimalHealthSystem extends NatureEntitySystem {

    public AnimalHealthSystem(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    public List<Class<? extends Component>> requiredComponents() {
        return List.of(HealthComponent.class, AgeComponent.class, MetabolismComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_LIFECYCLE;
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        // Safe cast due to requiredComponents filter
        Animal a = (Animal) entity;
        if (!a.isAlive()) {
            return;
        }

        Season season = getEnvironment().getCurrentSeason();
        int seasonMetabolismModifierBP = (int) (season.getMetabolismModifier() * config.getScale10K());

        // 1. Metabolism (Energy decay)
        long metabolism = a.getDynamicMetabolismRate();
        
        // Season global modifier
        metabolism = (metabolism * seasonMetabolismModifierBP) / config.getScale10K();

        // Hibernation: drastically reduce metabolism for cold-blooded in Winter
        if (season == Season.WINTER && a.getAnimalType().isColdBlooded()) {
            metabolism = (metabolism * config.getHibernationMetabolismModifierBP()) / config.getScale10K();
        }

        // Endangered protection: reduce metabolism if protected
        if (protectionMap != null && protectionMap.containsKey(a.getSpeciesKey())) {
            metabolism = (metabolism * (config.getScale10K() - 5000)) / config.getScale10K();
        }

        a.tryConsumeEnergy(metabolism);
        
        // 2. Age increment and death check
        if (a.isAlive()) {
            a.checkAgeDeath();
        }
    }
}
