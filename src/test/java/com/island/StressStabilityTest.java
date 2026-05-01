package com.island;

import static com.island.config.SimulationConstants.SCALE_1M;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.content.AnimalType;
import com.island.content.Organism;
import com.island.content.SimulationBootstrap;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class StressStabilityTest {

    @Test
    void testEcosystemStabilityFor500Ticks() {
        System.out.println("\n=== STARTING STRESS STABILITY TEST (500 TICKS) ===");
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext<Organism> context = bootstrap.setup();
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
