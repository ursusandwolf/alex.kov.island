package com.island.nature.integration;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.model.Island;
import com.island.nature.service.StatisticsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EcosystemBalanceTest {

    @Test
    @DisplayName("Long-term Stability: Ecosystem should survive 500 ticks without extinction")
    void ecosystem_should_be_stable_for_500_ticks() {
        Configuration config = new Configuration();
        config.setHeadless(true);
        config.setIslandWidth(10);
        config.setIslandHeight(10);
        config.setRandomSeed(42L); // Fixed seed for reproducibility
        
        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationConfig simConfig = SimulationConfig.defaultFor(4);
        
        try (SimulationContext<Organism> context = engine.build(plugin, simConfig)) {
            Island island = (Island) context.world();
            StatisticsService stats = plugin.getDomainContext().getStatisticsService();
            
            for (int i = 0; i < 500; i++) {
                context.gameLoop().runTick();
                
                // Every 100 ticks, verify all species still exist
                if ((i + 1) % 100 == 0) {
                    final int currentTick = i + 1;
                    Map<SpeciesKey, Integer> counts = stats.getSpeciesCountsMap();
                    counts.forEach((species, count) -> {
                        assertTrue(count > 0, "Species " + species + " went extinct at tick " + currentTick);
                    });
                }
            }
            
            // Final check
            Map<SpeciesKey, Integer> finalCounts = stats.getSpeciesCountsMap();
            assertFalse(finalCounts.isEmpty(), "Ecosystem should not be empty");
            finalCounts.forEach((species, count) -> {
                assertTrue(count > 0, "Species " + species + " went extinct in the end");
            });
        }
    }
}
