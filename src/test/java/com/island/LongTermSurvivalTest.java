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
        SpeciesRegistry registry = context.getSpeciesRegistry();
        
        context.getView().setSilent(true);

        int totalTicks = 500;
        for (int i = 1; i <= totalTicks; i++) {
            context.getGameLoop().runTick();
            
            if (i % 100 == 0) {
                Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
                long totalAnimals = counts.entrySet().stream()
                    .filter(e -> !registry.getAnimalType(e.getKey()).map(AnimalType::isBiomass).orElse(false))
                    .mapToLong(Map.Entry::getValue)
                    .sum();
                
                // Count producers as double mass
                double totalBiomass = 0;
                for (int x = 0; x < island.getWidth(); x++) {
                    for (int y = 0; y < island.getHeight(); y++) {
                        for (com.island.content.Biomass b : island.getGrid()[x][y].getBiomassContainers()) {
                            if (registry.getAnimalType(b.getSpeciesKey()).map(AnimalType::isPlant).orElse(false)) {
                                totalBiomass += b.getBiomass();
                            }
                        }
                    }
                }
                
                System.out.printf("Tick %d: Animals: %d, Producers Mass: %.2f, Species (%d): %s%n", 
                    i, totalAnimals, totalBiomass, counts.size(), counts.keySet());
                
                if (totalAnimals == 0) {
                    System.out.println("TOTAL DEATH STATISTICS:");
                    for (com.island.content.DeathCause cause : com.island.content.DeathCause.values()) {
                        Map<SpeciesKey, Integer> stats = island.getTotalDeathsBySpecies(cause);
                        if (!stats.isEmpty()) {
                            System.out.println("  " + cause + ": " + stats);
                        }
                    }
                }

                assertTrue(totalAnimals > 0, "Mass extinction at tick " + i);
            }
        }

        Map<SpeciesKey, Integer> finalCounts = island.getSpeciesCounts();
        System.out.println("Final state after 500 ticks: " + finalCounts);

        // Verification logic
        assertTrue(island.getTotalOrganismCount() > 0, "The island is dead!");
        
        // Sum total biomass again for final check
        double finalBiomass = 0;
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                for (com.island.content.Biomass b : island.getGrid()[x][y].getBiomassContainers()) {
                    if (registry.getAnimalType(b.getSpeciesKey()).map(AnimalType::isPlant).orElse(false)) {
                        finalBiomass += b.getBiomass();
                    }
                }
            }
        }

        assertTrue(finalBiomass > 0.1, "Plants were overgrazed to extinction! Mass: " + finalBiomass);
        
        System.out.println("Ecosystem successfully survived 500 ticks.");
    }
}
