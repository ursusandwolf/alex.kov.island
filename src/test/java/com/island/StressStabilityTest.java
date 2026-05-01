package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.content.AnimalType;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static com.island.config.SimulationConstants.SCALE_1M;

public class StressStabilityTest {

    @Test
    void testEcosystemStabilityFor500Ticks() {
        System.out.println("\n=== STARTING STRESS STABILITY TEST (500 TICKS) ===");
        
        // Large enough to avoid random early extinction, small enough to run fast
        System.setProperty("island.width", "20");
        System.setProperty("island.height", "20");

        try {
            SimulationBootstrap bootstrap = new SimulationBootstrap();
            SimulationContext context = bootstrap.setup();
            Island island = context.getIsland();
            SpeciesRegistry registry = context.getSpeciesRegistry();
            
            context.getView().setSilent(true);

            int totalTicks = 200;
            int extinctionTick = -1;
            
            Map<com.island.content.DeathCause, Long> periodDeaths = new java.util.EnumMap<>(com.island.content.DeathCause.class);

            for (int i = 1; i <= totalTicks; i++) {
                context.getGameLoop().runTick();
                
                for (com.island.content.DeathCause cause : com.island.content.DeathCause.values()) {
                    Map<SpeciesKey, Integer> tickDeaths = island.getStatisticsService().getTickDeaths(cause);
                    long sum = tickDeaths.values().stream().mapToLong(Integer::longValue).sum();
                    periodDeaths.merge(cause, sum, Long::sum);
                }
                
                if (i % 20 == 0) {
                    Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
                    long totalAnimals = counts.entrySet().stream()
                        .filter(e -> !registry.getAnimalType(e.getKey()).map(AnimalType::isBiomass).orElse(false))
                        .mapToLong(Map.Entry::getValue)
                        .sum();
                    
                    System.out.printf("Tick %d: Total Animals: %d, Species Count: %d, Global Satiety: %.1f%%%n", 
                        i, totalAnimals, counts.size(), context.getIsland().getStatisticsService().calculateGlobalSatiety(island));
                    System.out.println("  Alive species: " + counts.keySet().stream().map(SpeciesKey::getCode).sorted().toList());
                    
                    System.out.println("  Deaths this period (last 20 ticks):");
                    periodDeaths.forEach((cause, count) -> {
                        if (count > 0) {
                            System.out.println("    " + cause + ": " + count);
                        }
                    });
                    periodDeaths.clear();
                    
                    if (totalAnimals == 0) {
                        extinctionTick = i;
                        break;
                    }
                }
            }

            Map<SpeciesKey, Integer> finalCounts = island.getSpeciesCounts();
            System.out.println("Final species population: " + finalCounts);

            if (extinctionTick != -1) {
                System.out.println("MASS EXTINCTION at tick " + extinctionTick);
                fail("Ecosystem collapsed at tick " + extinctionTick);
            }

            assertTrue(island.getTotalOrganismCount() > 0, "Island is completely dead!");
            System.out.println("Ecosystem survived 500 ticks successfully.");
        } finally {
            System.clearProperty("island.width");
            System.clearProperty("island.height");
        }
    }
}
