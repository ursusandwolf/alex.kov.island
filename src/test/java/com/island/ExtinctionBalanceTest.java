package com.island;

import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.model.Island;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;

/**
 * Diagnostic test to identify species that go extinct too often.
 * Runs multiple iterations to find patterns in extinctions.
 */
public class ExtinctionBalanceTest {

    private static final int MAX_TICKS = 50;
    private static final int ITERATIONS = 10;

    @Test
    void findExtinctionProneSpecies() {
        Map<SpeciesKey, AtomicInteger> extinctionStats = new HashMap<>();

        for (int i = 0; i < ITERATIONS; i++) {
            Configuration config = Configuration.load();
            NaturePlugin plugin = new NaturePlugin(config);
            SimulationEngine<Organism> engine = new SimulationEngine<>();
            SimulationContext<Organism> context = engine.build(plugin, config.getTickDurationMs(), 4);
            
            GameLoop<Organism> gameLoop = context.gameLoop();

            Set<SpeciesKey> initiallyPresent = ((NatureWorld) context.world()).getRegistry().getAllAnimalKeys();

            for (int t = 0; t < MAX_TICKS; t++) {
                gameLoop.runTick();
            }

            Map<SpeciesKey, Integer> counts = ((Island) context.world()).getSpeciesCounts();
            for (SpeciesKey key : initiallyPresent) {
                if (counts.getOrDefault(key, 0) == 0) {
                    extinctionStats.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
                }
            }
            gameLoop.stop();
        }

        System.out.println("\n=== EXTINCTION STATS (after " + ITERATIONS + " runs) ===");
        extinctionStats.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<SpeciesKey, AtomicInteger> e) -> e.getValue().get()).reversed())
                .forEach(e -> System.out.println(e.getKey().getCode() + ": " + e.getValue().get() + " extinctions"));
    }
}