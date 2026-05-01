package com.island.nature.service;

import com.island.nature.config.SimulationConstants;
import com.island.nature.entities.Animal;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureEnvironment;
import com.island.nature.entities.NatureStatistics;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.Season;
import com.island.nature.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for aging, energy decay, and natural growth/death using integer arithmetic.
 */
public class LifecycleService extends AbstractService {

    private final NatureStatistics statistics;
    private final NatureEnvironment environment;

    public LifecycleService(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.statistics = world;
        this.environment = world;
    }

    public LifecycleService(NatureStatistics statistics, NatureEnvironment environment, 
                            ExecutorService executor, RandomProvider random) {
        // Fallback for when we don't have a full NatureWorld, but AbstractService needs it for tick()
        // We'll address AbstractService later. For now, keep it compatible.
        super(null, executor, random); 
        this.statistics = statistics;
        this.environment = environment;
    }

    @Override
    public void processCell(Cell cell, int tickCount) {
        processAging(cell);
        processBiomassGrowth(cell);
    }

    private void processAging(Cell node) {
        Season season = environment.getCurrentSeason();
        int seasonMetabolismModifierBP = (int) (season.getMetabolismModifier() * SimulationConstants.SCALE_10K);

        node.forEachAnimal(a -> {
            if (a.isAlive()) {
                // 1. Metabolism (Energy decay)
                long metabolism = a.getDynamicMetabolismRate();
                
                // Season global modifier
                metabolism = (metabolism * seasonMetabolismModifierBP) / SimulationConstants.SCALE_10K;

                // Hibernation: drastically reduce metabolism for cold-blooded in Winter
                if (season == Season.WINTER && a.getAnimalType().isColdBlooded()) {
                    metabolism = (metabolism * SimulationConstants.HIBERNATION_METABOLISM_MODIFIER_BP) / SimulationConstants.SCALE_10K;
                }

                // Endangered protection: reduce metabolism if protected
                if (protectionMap != null && protectionMap.containsKey(a.getSpeciesKey())) {
                    metabolism = (metabolism * (SimulationConstants.SCALE_10K - 5000)) / SimulationConstants.SCALE_10K;
                }

                if (!a.tryConsumeEnergy(metabolism)) {
                    statistics.reportDeath(a.getSpeciesKey(), DeathCause.HUNGER);
                }
                
                // 2. Age increment and death check
                if (a.isAlive() && a.checkAgeDeath()) {
                    statistics.reportDeath(a.getSpeciesKey(), DeathCause.AGE);
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
