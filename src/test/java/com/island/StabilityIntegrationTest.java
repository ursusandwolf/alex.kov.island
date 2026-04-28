package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.content.SpeciesKey;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class StabilityIntegrationTest {

    @Test
    void runStabilityCheck() {
        System.out.println("=== STABILITY CHECK REPORT (40 Ticks) ===");
        System.out.println("Survived | Extinct Species");
        System.out.println("------------------------------------------");

        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup();
        context.getView().setSilent(true);

        for (int i = 0; i < 40; i++) {
            context.getGameLoop().runTick();
            if (context.getIsland().getTotalOrganismCount() == 0) break;
        }

        Map<SpeciesKey, Integer> counts = context.getIsland().getSpeciesCounts();
        
        // Assertions
        org.junit.jupiter.api.Assertions.assertTrue(counts.getOrDefault(SpeciesKey.GRASS, 0) > 0, 
            "Grass must survive - ecosystem collapses without primary producers");
        org.junit.jupiter.api.Assertions.assertTrue(counts.getOrDefault(SpeciesKey.CABBAGE, 0) > 0, 
            "Cabbage must survive - ecosystem collapses without primary producers");
        
        long predatorsCount = counts.entrySet().stream()
            .filter(e -> e.getKey().isPredator() && e.getValue() > 0)
            .count();
        org.junit.jupiter.api.Assertions.assertTrue(predatorsCount >= 1, 
            "At least one predator species must survive");

        System.out.println(formatReport(counts));
    }

    private String formatReport(Map<SpeciesKey, Integer> counts) {
        int survivedCount = 0;
        StringBuilder extinct = new StringBuilder();
        
        for (SpeciesKey species : SpeciesKey.values()) {
            if (species == SpeciesKey.PLANT) continue; // Category, not a species
            
            if (counts.getOrDefault(species, 0) > 0) {
                survivedCount++;
            } else {
                if (extinct.length() > 0) extinct.append(", ");
                extinct.append(species.getCode());
            }
        }
        return String.format("%-8d | %s", survivedCount, extinct);
    }
}
