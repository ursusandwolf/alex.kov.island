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

        String result = runSimulationSession(40);
        System.out.println(result);
    }

    private String runSimulationSession(int maxTicks) {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup(); 

        context.getConsoleView().setSilent(true);

        for (int i = 0; i < maxTicks; i++) {
            context.getGameLoop().runTick();
            if (context.getIsland().getTotalOrganismCount() == 0) break;
        }

        Map<SpeciesKey, Integer> counts = context.getIsland().getSpeciesCounts();
        int survivedCount = 0;
        StringBuilder extinct = new StringBuilder();
        
        for (SpeciesKey species : SpeciesKey.values()) {
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
