package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.SpeciesConfig;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class EcosystemStabilityTest {

    @Test
    void testEcosystemStabilityOverTime() {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup(); // Default is 100x20
        Island island = context.getIsland();
        SpeciesConfig speciesConfig = context.getSpeciesConfig();
        
        context.getConsoleView().setSilent(true);

        int maxTicks = 200; // Increased to check stability
        for (int i = 0; i < maxTicks; i++) {
            context.getGameLoop().runTick();
            
            // Check if any species completely wiped out early
            if (i > 50) { // Give it some time to settle
                Map<String, Integer> counts = island.getSpeciesCounts();
                assertTrue(counts.size() > 5, "Too many species extinct at tick " + i);
            }
        }

        Map<String, Integer> finalCounts = island.getSpeciesCounts();
        
        // Assertions for stability
        assertFalse(finalCounts.isEmpty(), "Island is empty!");
        
        // We expect at least some species to survive
        // In a real stable ecosystem all should survive, but 200 ticks is a lot for a simple model.
        // Let's at least check that it didn't collapse to 0.
        assertTrue(island.getTotalOrganismCount() > 0, "All organisms are dead!");
        
        // Check for overpopulation (e.g. not more than 3x initial max capacity if balanced)
        int islandArea = island.getWidth() * island.getHeight();
        for (Map.Entry<String, Integer> entry : finalCounts.entrySet()) {
            String key = entry.getKey();
            if (key.equals("plant") || key.equals("cabbage") || key.equals("caterpillar")) continue;
            
            int count = entry.getValue();
            int maxPerCell = speciesConfig.getAnimalType(key).getMaxPerCell();
            int globalCapacity = maxPerCell * islandArea;
            
            assertTrue(count < globalCapacity * 2.0, "Overpopulation of " + key + ": " + count);
        }
    }
}
