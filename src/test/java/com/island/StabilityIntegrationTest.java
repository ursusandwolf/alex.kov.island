package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.config.SimulationConstants;
import com.island.config.Configuration;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class StabilityIntegrationTest {

    @Test
    void runStabilitySweep() {
        double[] reproThresholds = {70.0, 90.0};
        double[] metabolismRates = {0.10, 0.15};

        System.out.println("=== STABILITY SWEEP REPORT (40 Ticks) ===");
        System.out.println("Repro% | Metal% | Survived | Extinct Species");
        System.out.println("------------------------------------------");

        for (double repro : reproThresholds) {
            for (double metabolism : metabolismRates) {
                SimulationConstants.REPRODUCTION_MIN_ENERGY_PERCENT = repro;
                SimulationConstants.BASE_METABOLISM_PERCENT = metabolism;

                String result = runSimulationSession(40);
                System.out.printf("%-6.1f | %-6.2f | %s\n", repro, metabolism, result);
            }
        }
    }

    private String runSimulationSession(int maxTicks) {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup(); // Use default config (100x20)

        // Disable console output during stability sweep for performance
        context.getConsoleView().setSilent(true);

        for (int i = 0; i < maxTicks; i++) {
            context.getGameLoop().runTick();
            if (context.getIsland().getTotalOrganismCount() == 0) break;
        }

        Map<String, Integer> counts = context.getIsland().getSpeciesCounts();
        int survivedCount = 0;
        StringBuilder extinct = new StringBuilder();
        
        for (String species : context.getSpeciesConfig().getAllSpeciesKeys()) {
            if (counts.getOrDefault(species, 0) > 0) {
                survivedCount++;
            } else {
                if (extinct.length() > 0) extinct.append(", ");
                extinct.append(species);
            }
        }
        return String.format("%-8d | %s", survivedCount, extinct);
    }
}
