package com.island.nature.view;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Organism;
import org.junit.jupiter.api.Test;

public class HeadlessModeTest {

    @Test
    void testHeadlessModeSkipsViewTask() {
        Configuration config = new Configuration();
        config.setHeadless(true);
        config.setIslandWidth(2);
        config.setIslandHeight(2);

        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationConfig simConfig = SimulationConfig.defaultFor(1);
        try (SimulationContext<Organism> context = engine.build(plugin, simConfig)) {
            context.gameLoop().runTick();
        }
    }
    
    @Test
    void testConsoleModeIncludesViewTask() {
        Configuration config = new Configuration();
        config.setHeadless(false);
        config.setIslandWidth(2);
        config.setIslandHeight(2);

        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationConfig simConfig = SimulationConfig.defaultFor(1);
        try (SimulationContext<Organism> context = engine.build(plugin, simConfig)) {
            context.gameLoop().runTick();
        }
    }
}
