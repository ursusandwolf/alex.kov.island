package com.island;

import static com.island.nature.config.SimulationConstants.SCALE_1M;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SimulationBootstrap;
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
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext<Organism> context = bootstrap.setup();
        context.getView().setSilent(true);
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
