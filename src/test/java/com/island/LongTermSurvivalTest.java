package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class LongTermSurvivalTest {

    @Test
    void testStabilityFor500Ticks() {
        System.out.println("\n=== STARTING LONG-TERM SURVIVAL TEST (500 TICKS) ===");
        
        // Use a smaller island for faster testing, but large enough for genetic diversity
        System.setProperty("island.width", "10");
        System.setProperty("island.height", "10");

        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup();
        Island island = context.getIsland();
        
        context.getView().setSilent(true);

        int totalTicks = 500;
        for (int i = 1; i <= totalTicks; i++) {
            context.getGameLoop().runTick();
            
            if (i % 100 == 0) {
                Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
                long totalAnimals = counts.entrySet().stream()
                    .filter(e -> !e.getKey().equals(SpeciesKey.GRASS) && !e.getKey().equals(SpeciesKey.CABBAGE))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                
                System.out.printf("Tick %d: Animals: %d, Producers: %d, Species: %d%n", 
                    i, totalAnimals, 
                    counts.getOrDefault(SpeciesKey.GRASS, 0) + counts.getOrDefault(SpeciesKey.CABBAGE, 0),
                    counts.size());
                
                assertTrue(totalAnimals > 0, "Mass extinction at tick " + i);
            }
        }

        Map<SpeciesKey, Integer> finalCounts = island.getSpeciesCounts();
        System.out.println("Final state after 500 ticks: " + finalCounts);

        // Verification logic
        assertTrue(island.getTotalOrganismCount() > 0, "The island is dead!");
        
        long herbivoreCount = finalCounts.entrySet().stream()
            .filter(e -> {
                var type = context.getSpeciesRegistry().getAnimalType(e.getKey());
                return type.isPresent() && !type.get().getPreySpecies().isEmpty() && 
                       (type.get().canEat(SpeciesKey.GRASS) || type.get().canEat(SpeciesKey.PLANT));
            })
            .mapToLong(Map.Entry::getValue)
            .sum();

        assertTrue(herbivoreCount > 0, "No herbivores survived - ecosystem collapsed!");
        
        int producers = finalCounts.getOrDefault(SpeciesKey.GRASS, 0) + finalCounts.getOrDefault(SpeciesKey.CABBAGE, 0);
        assertTrue(producers > 100, "Plants were overgrazed to near extinction! Count: " + producers);
        
        System.out.println("Ecosystem successfully survived 500 ticks.");
    }
}
