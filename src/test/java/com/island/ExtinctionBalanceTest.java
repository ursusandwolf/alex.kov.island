package com.island;

import com.island.content.SpeciesKey;
import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.engine.GameLoop;
import com.island.content.DeathCause;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Diagnostic test to identify species that go extinct too often.
 * Runs multiple iterations to find patterns in extinctions.
 */
public class ExtinctionBalanceTest {

    private static final int MAX_TICKS = 50;
    private static final int MAX_ITERATIONS = 20;
    private static final int EXTINCTION_THRESHOLD = 10;

    @Test
    void debugSpeciesExtinctionPatterns() {
        Map<SpeciesKey, Integer> extinctionCounts = new HashMap<>();
        Map<SpeciesKey, Integer> consecutiveExtinctions = new HashMap<>();
        SpeciesKey lastExtinct = null;

        System.out.println("=== STARTING EXTINCTION BALANCE TEST ===");
        System.out.println("Goal: Find species that go extinct frequently within " + MAX_TICKS + " ticks.");

        for (int i = 1; i <= MAX_ITERATIONS; i++) {
            SimulationBootstrap bootstrap = new SimulationBootstrap();
            SimulationContext context = bootstrap.setup();
            GameLoop gameLoop = context.getGameLoop();

            SpeciesKey extinctInThisRun = runUntilExtinction(context, MAX_TICKS);
            
            if (extinctInThisRun != null) {
                extinctionCounts.merge(extinctInThisRun, 1, Integer::sum);
                
                if (extinctInThisRun.equals(lastExtinct)) {
                    consecutiveExtinctions.merge(extinctInThisRun, 1, Integer::sum);
                } else {
                    consecutiveExtinctions.put(extinctInThisRun, 1);
                }
                
                lastExtinct = extinctInThisRun;
                
                int consecutive = consecutiveExtinctions.getOrDefault(extinctInThisRun, 0);
                System.out.printf("Iteration %d: Species '%s' went extinct. (Total: %d, Consecutive: %d)%n", 
                        i, extinctInThisRun.getCode(), extinctionCounts.get(extinctInThisRun), consecutive);

                if (consecutive >= EXTINCTION_THRESHOLD) {
                    System.out.printf("!!! ALERT: Species '%s' extinct %d times in a row! Stopping test early.%n", 
                            extinctInThisRun.getCode(), consecutive);
                    break;
                }
            } else {
                lastExtinct = null;
                System.out.printf("Iteration %d: No species went extinct within %d ticks.%n", i, MAX_TICKS);
            }
        }

        printFinalReport(extinctionCounts);
    }

    private SpeciesKey runUntilExtinction(SimulationContext context, int maxTicks) {
        GameLoop gameLoop = context.getGameLoop();
        
        for (int tick = 1; tick <= maxTicks; tick++) {
            gameLoop.runTick();
            
            Map<SpeciesKey, Integer> counts = context.getIsland().getSpeciesCounts();
            for (SpeciesKey species : context.getSpeciesRegistry().getAllAnimalKeys()) {
                // Ignore insects/plants modeled as biomass if they are in animal keys
                if (context.getSpeciesRegistry().getAnimalType(species)
                        .map(t -> t.isBiomass()).orElse(false)) {
                    continue;
                }
                
                if (counts.getOrDefault(species, 0) <= 0) {
                    return species;
                }
            }
        }
        return null;
    }

    private void printFinalReport(Map<SpeciesKey, Integer> extinctionCounts) {
        System.out.println("\n=== FINAL EXTINCTION REPORT ===");
        if (extinctionCounts.isEmpty()) {
            System.out.println("No extinctions recorded. Ecosystem seems stable for 50 ticks.");
        } else {
            extinctionCounts.entrySet().stream()
                    .sorted(Map.Entry.<SpeciesKey, Integer>comparingByValue().reversed())
                    .forEach(e -> System.out.printf("Species: %-10s | Extinctions: %d%n", e.getKey().getCode(), e.getValue()));
        }
        System.out.println("===============================");
    }
}
