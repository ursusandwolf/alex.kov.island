package com.island;

import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.model.Island;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.nature.entities.core.Organism;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

@Tag("performance")
public class ExtremeScalePerformanceTest {

    @Test
    void benchmark20x20Grid() {
        System.out.println("\n=== STARTING LARGE SCALE PERFORMANCE TEST (20x20) ===");
        Configuration config = Configuration.load();
        config.setIslandWidth(20);
        config.setIslandHeight(20);
        config.setDynamicChunkingEnabled(true);
        config.setRebalanceInterval(5);

        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        
        // Use all available cores (0 means virtual threads in SimulationEngine)
        SimulationContext<Organism> context = engine.build(plugin, 0, 0);
        Island island = (Island) context.world();

        System.out.println("Initial Population: " + island.getTotalOrganismCount());
        
        long startTime = System.nanoTime();
        int ticks = 50;
        
        for (int i = 1; i <= ticks; i++) {
            long tickStart = System.nanoTime();
            context.gameLoop().runTick();
            long tickEnd = System.nanoTime();
            
            if (i % 10 == 0) {
                System.out.printf("Tick %d: Population=%d, Duration=%d ms, Temp=%d C, Season=%s%n", 
                    i, island.getTotalOrganismCount(), TimeUnit.NANOSECONDS.toMillis(tickEnd - tickStart),
                    island.getTemperature(), island.getCurrentSeason());
            }
        }
        
        long endTime = System.nanoTime();
        long totalDurationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        System.out.println("Benchmark Finished.");
        System.out.println("Total Ticks: " + ticks);
        System.out.println("Total Duration: " + totalDurationMs + " ms");
        System.out.printf("Average TPS: %.2f%n", (double) ticks / (totalDurationMs / 1000.0));
    }
}
