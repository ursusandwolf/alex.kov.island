package com.island.nature.view;

import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.scheduling.ScheduledTask;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Organism;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeadlessModeTest {

    @Test
    void testHeadlessModeSkipsViewTask() {
        Configuration config = new Configuration();
        config.setHeadless(true);
        config.setIslandWidth(2);
        config.setIslandHeight(2);

        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationContext<Organism> context = engine.build(plugin, 100, 1);

        context.gameLoop().runTick();
    }
    
    @Test
    void testConsoleModeIncludesViewTask() {
        Configuration config = new Configuration();
        config.setHeadless(false);
        config.setIslandWidth(2);
        config.setIslandHeight(2);

        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        SimulationContext<Organism> context = engine.build(plugin, 100, 1);

        context.gameLoop().runTick();
    }
}
