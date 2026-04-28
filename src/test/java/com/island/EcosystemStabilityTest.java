package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

public class EcosystemStabilityTest {

    @Test
    void testEcosystemStabilityOverTime() {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup(); // Default is 100x20
        Island island = context.getIsland();
        SpeciesRegistry speciesRegistry = context.getSpeciesRegistry();
        
        context.getView().setSilent(true);

        int maxTicks = 200; 
        for (int i = 0; i < maxTicks; i++) {
            context.getGameLoop().runTick();
            
            if (i > 50) { 
                Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
                assertTrue(counts.size() > 5, "Too many species extinct at tick " + i);
            }
        }

        Map<SpeciesKey, Integer> finalCounts = island.getSpeciesCounts();
        
        assertFalse(finalCounts.isEmpty(), "Island is empty!");
        assertTrue(island.getTotalOrganismCount() > 0, "All organisms are dead!");
        
        int islandArea = island.getWidth() * island.getHeight();
        for (Map.Entry<SpeciesKey, Integer> entry : finalCounts.entrySet()) {
            SpeciesKey key = entry.getKey();
            if (key == SpeciesKey.PLANT || key == SpeciesKey.CABBAGE || key == SpeciesKey.CATERPILLAR || key == SpeciesKey.GRASS) continue;
            
            int count = entry.getValue();
            int maxPerCell = speciesRegistry.getAnimalType(key).orElseThrow().getMaxPerCell();
            int globalCapacity = maxPerCell * islandArea;
            
            assertTrue(count < globalCapacity * 3.0, "Overpopulation of " + key + ": " + count);
        }
    }
}
