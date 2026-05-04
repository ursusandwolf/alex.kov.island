package com.island.nature.service;

import com.island.engine.SimulationNode;
import com.island.engine.event.EntityDiedEvent;
import com.island.engine.event.EventBus;
import com.island.nature.entities.Animal;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureEnvironment;
import com.island.nature.entities.NatureStatistics;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.Season;
import com.island.nature.entities.TaskRegistry;
import com.island.nature.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for aging, energy decay, and natural growth/death using integer arithmetic.
 */
public class LifecycleService extends AbstractService {

    private final NatureStatistics statistics;
    private final NatureEnvironment environment;
    private final EventBus eventBus;

    public LifecycleService(NatureWorld world, ExecutorService executor, RandomProvider random, EventBus eventBus) {
        super(world, executor, random);
        this.statistics = world;
        this.environment = world;
        this.eventBus = eventBus;
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_LIFECYCLE;
    }

    @Override
    public void processCell(SimulationNode<Organism> node, int tickCount) {
        if (node instanceof Cell cell) {
            processAging(cell);
            processBiomassGrowth(cell);
        }
    }

    private void processAging(Cell node) {
        Season season = environment.getCurrentSeason();
        int seasonMetabolismModifierBP = (int) (season.getMetabolismModifier() * config.getScale10K());

        node.forEachAnimal(a -> {
            if (a.isAlive()) {
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
                if (!a.isAlive()) {
                    eventBus.publish(new EntityDiedEvent(a, DeathCause.HUNGER.name()));
                }
                
                // 2. Age increment and death check
                if (a.isAlive()) {
                    if (a.checkAgeDeath()) {
                        eventBus.publish(new EntityDiedEvent(a, DeathCause.AGE.name()));
                    }
                }
            }
        });
    }

    private void processBiomassGrowth(Cell node) {
        Season season = environment.getCurrentSeason();
        double growthModifier = season.getGrowthModifier();
        
        node.forEachEntity(e -> {
            if (e instanceof Biomass b && b.isAlive()) {
                b.grow(node, growthModifier);
            }
        });
    }
}
