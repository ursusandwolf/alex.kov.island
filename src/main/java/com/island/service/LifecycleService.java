package com.island.service;

import com.island.engine.SimulationNode;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.NatureWorld;
import com.island.content.Organism;
import com.island.content.Season;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for aging, energy decay, and natural growth/death using integer arithmetic.
 */
public class LifecycleService extends AbstractService {

    public LifecycleService(NatureWorld world, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
    }

    @Override
    public void processCell(SimulationNode<Organism> node, int tickCount) {
        if (node instanceof Cell cell) {
            processAging(cell);
            processBiomassGrowth(cell);
        }
    }

    private void processAging(Cell node) {
        Season season = getWorld().getCurrentSeason();
        int seasonMetabolismModifierBP = (int) (season.getMetabolismModifier() * com.island.config.SimulationConstants.SCALE_10K);

        node.forEachAnimal(a -> {
            if (a.isAlive()) {
                // 1. Metabolism (Energy decay)
                long metabolism = a.getDynamicMetabolismRate();
                
                // Season global modifier
                metabolism = (metabolism * seasonMetabolismModifierBP) / com.island.config.SimulationConstants.SCALE_10K;

                // Hibernation: drastically reduce metabolism for cold-blooded in Winter
                if (season == Season.WINTER && a.getAnimalType().isColdBlooded()) {
                    metabolism = (metabolism * com.island.config.SimulationConstants.HIBERNATION_METABOLISM_MODIFIER_BP) / com.island.config.SimulationConstants.SCALE_10K;
                }

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

    private void processBiomassGrowth(Cell node) {
        Season season = getWorld().getCurrentSeason();
        double growthModifier = season.getGrowthModifier();
        
        node.forEachEntity(e -> {
            if (e instanceof Biomass b && b.isAlive()) {
                b.grow(node, growthModifier);
            }
        });
    }
}
