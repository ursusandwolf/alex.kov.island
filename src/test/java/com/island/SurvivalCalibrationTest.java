package com.island;

import com.island.engine.GameLoop;
import com.island.engine.SimulationContext;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.model.Island;
import com.island.nature.service.StatisticsService;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Targeted calibration test for apex predators and large herbivores.
 * Monitors survival trends and mortality causes to optimize parameters.
 */
public class SurvivalCalibrationTest {

    private static final int MAX_TICKS = 100;
    private static final int ITERATIONS = 5;
    
    private static final List<SpeciesKey> TARGET_SPECIES = List.of(
            SpeciesKey.WOLF, SpeciesKey.BEAR, 
            SpeciesKey.HORSE, SpeciesKey.DEER, SpeciesKey.BUFFALO
    );

    @Test
    void calibrateTargetSpeciesSurvival() {
        System.out.println("\n=== STARTING SURVIVAL CALIBRATION TEST ===");
        Map<SpeciesKey, SpeciesStats> globalStats = new HashMap<>();
        TARGET_SPECIES.forEach(k -> globalStats.put(k, new SpeciesStats()));

        for (int i = 0; i < ITERATIONS; i++) {
            com.island.nature.config.Configuration config = com.island.nature.config.Configuration.load();
            config.setIslandWidth(10);
            config.setIslandHeight(10);
            
            com.island.nature.NaturePlugin plugin = new com.island.nature.NaturePlugin(config);
            com.island.engine.SimulationEngine<Organism> engine = new com.island.engine.SimulationEngine<>();
            SimulationContext<Organism> context = engine.build(plugin, config.getTickDurationMs(), 4);
            
            Island island = (Island) context.getWorld();
            GameLoop<Organism> gameLoop = context.getGameLoop();

            for (int t = 0; t < MAX_TICKS; t++) {
                gameLoop.runTick();
            }

            StatisticsService stats = island.getStatisticsService();
            Map<SpeciesKey, Integer> finalCounts = stats.getSpeciesCountsMap();
            
            Map<SpeciesKey, Integer> hungerDeaths = stats.getTotalDeaths(DeathCause.HUNGER);
            Map<SpeciesKey, Integer> ageDeaths = stats.getTotalDeaths(DeathCause.AGE);
            
            for (SpeciesKey key : TARGET_SPECIES) {
                int count = finalCounts.getOrDefault(key, 0);
                SpeciesStats s = globalStats.get(key);
                s.totalEndPopulation.addAndGet(count);
                if (count == 0) {
                    s.extinctions.incrementAndGet();
                }
                
                s.hungerDeaths.addAndGet(hungerDeaths.getOrDefault(key, 0));
                s.ageDeaths.addAndGet(ageDeaths.getOrDefault(key, 0));
            }
            gameLoop.stop();
        }

        printResults(globalStats);
    }

    private void printResults(Map<SpeciesKey, SpeciesStats> stats) {
        System.out.println(String.format("%-10s | %-12s | %-12s | %-12s | %-12s", 
                "Species", "Avg Pop", "Extinct Rate", "Hunger %", "Age %"));
        System.out.println("-----------|--------------|--------------|--------------|--------------");

        for (SpeciesKey key : TARGET_SPECIES) {
            SpeciesStats s = stats.get(key);
            double avgPop = (double) s.totalEndPopulation.get() / ITERATIONS;
            double extinctRate = (double) s.extinctions.get() / ITERATIONS;
            long totalDeaths = s.hungerDeaths.get() + s.ageDeaths.get();
            double hungerPct = totalDeaths > 0 ? (double) s.hungerDeaths.get() * 100 / totalDeaths : 0;
            double agePct = totalDeaths > 0 ? (double) s.ageDeaths.get() * 100 / totalDeaths : 0;

            System.out.println(String.format("%-10s | %-12.2f | %-12.2f | %-12.1f%% | %-12.1f%%", 
                    key.getCode(), avgPop, extinctRate, hungerPct, agePct));
        }
        
        System.out.println("\n--- Calibration Recommendations ---");
        for (SpeciesKey key : TARGET_SPECIES) {
            SpeciesStats s = stats.get(key);
            double extinctRate = (double) s.extinctions.get() / ITERATIONS;
            long totalDeaths = s.hungerDeaths.get() + s.ageDeaths.get();
            double hungerPct = totalDeaths > 0 ? (double) s.hungerDeaths.get() * 100 / totalDeaths : 0;
            
            if (extinctRate > 0.3) {
                System.out.print(key.getCode() + ": CRITICAL. ");
                if (hungerPct > 60) {
                    System.out.println("Increase hunting/feeding success or decrease metabolism.");
                } else {
                    System.out.println("Increase reproduction chance or offspring count.");
                }
            } else if (extinctRate > 0) {
                System.out.println(key.getCode() + ": STABLE BUT VULNERABLE. Minor buffs recommended.");
            } else {
                System.out.println(key.getCode() + ": HEALTHY.");
            }
        }
    }

    private static class SpeciesStats {
        AtomicLong totalEndPopulation = new AtomicLong(0);
        AtomicInteger extinctions = new AtomicInteger(0);
        AtomicLong hungerDeaths = new AtomicLong(0);
        AtomicLong ageDeaths = new AtomicLong(0);
    }
}
