package com.island;

import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.model.Island;
import java.util.Map;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.registry.SpeciesRegistry;

public class StressStabilityTest {

    @Test
    void testEcosystemStabilityFor200Ticks() {
        System.out.println("\n=== STARTING STRESS STABILITY TEST (200 TICKS) ===");
        Configuration config = Configuration.load();
        // Use a smaller world for the test to avoid extreme slowdowns
        config.setIslandWidth(5);
        config.setIslandHeight(5);
        
        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationContext<Organism> context = engine.build(plugin, config.getTickDurationMs(), 4);
        
        Island island = (Island) context.world();

        for (int i = 0; i < 200; i++) {
            context.gameLoop().runTick();
            
            if (i % 50 == 0) {
                System.out.println("Tick " + i + ": Population=" + island.getTotalOrganismCount();
            }
        }

        System.out.println("Final Population: " + island.getTotalOrganismCount();
        assertTrue(island.getTotalOrganismCount() > 0, "Ecosystem should not be empty after 200 ticks");
    }
}