package com.island;

import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SimulationBootstrap;
import com.island.nature.entities.SpeciesKey;
import com.island.engine.GameLoop;
import com.island.engine.SimulationContext;
import com.island.nature.model.Island;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

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
            if (context.getView() != null) {
                context.getView().setSilent(true);
            }
            GameLoop<Organism> gameLoop = context.getGameLoop();

            Set<SpeciesKey> initiallyPresent = ((NatureWorld) context.getWorld()).getRegistry().getAllAnimalKeys();

            for (int t = 0; t < MAX_TICKS; t++) {
                gameLoop.runTick();
            }

            Map<SpeciesKey, Integer> counts = ((Island) context.getWorld()).getSpeciesCounts();
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
