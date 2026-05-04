package com.island;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesRegistry;
import com.island.engine.SimulationContext;
import com.island.nature.model.Island;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

public class StressStabilityTest {

    @Test
    @Disabled("Long running stress test")
    void testEcosystemStabilityFor500Ticks() {
        System.out.println("\n=== STARTING STRESS STABILITY TEST (500 TICKS) ===");
        com.island.nature.config.Configuration config = com.island.nature.config.Configuration.load();
        com.island.nature.NaturePlugin plugin = new com.island.nature.NaturePlugin(config);
        com.island.engine.SimulationEngine<Organism> engine = new com.island.engine.SimulationEngine<>();
        SimulationContext<Organism> context = engine.build(plugin, config.getTickDurationMs(), 4);
        
        Island island = (Island) context.getWorld();

        for (int i = 0; i < 500; i++) {
            context.getGameLoop().runTick();
            
            if (i % 100 == 0) {
                System.out.println("Tick " + i + ": Population=" + island.getTotalOrganismCount());
            }
        }

        System.out.println("Final Population: " + island.getTotalOrganismCount());
        assertTrue(island.getTotalOrganismCount() > 0, "Ecosystem should not be empty after 500 ticks");
    }
}
