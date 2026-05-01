package com.island;

import com.island.content.SpeciesKey;
import com.island.content.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.content.Organism;
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
    private static final int ITERATIONS = 3;

    @Test
    void findExtinctionProneSpecies() {
        Map<SpeciesKey, AtomicInteger> extinctionStats = new HashMap<>();

        for (int i = 0; i < ITERATIONS; i++) {
            SimulationBootstrap bootstrap = new SimulationBootstrap();
            SimulationContext<Organism> context = bootstrap.setup();
            GameLoop<Organism> gameLoop = context.getGameLoop();

            Set<SpeciesKey> initiallyPresent = ((com.island.content.NatureWorld) context.getWorld()).getRegistry().getAllAnimalKeys();

            for (int t = 0; t < MAX_TICKS; t++) {
                gameLoop.runTick();
            }

            Map<SpeciesKey, Integer> counts = ((com.island.model.Island) context.getWorld()).getSpeciesCounts();
            for (SpeciesKey key : initiallyPresent) {
                if (counts.getOrDefault(key, 0) == 0) {
                    extinctionStats.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                }
            }
        }

        System.out.println("\n=== EXTINCTION STATS (after " + ITERATIONS + " runs) ===");
        extinctionStats.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<SpeciesKey, AtomicInteger> e) -> e.getValue().get()).reversed())
                .forEach(e -> System.out.println(e.getKey().getCode() + ": " + e.getValue().get() + " extinctions"));
    }
}
