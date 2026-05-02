package com.island.nature.entities;

import com.island.engine.SimulationContext;
import com.island.engine.SimulationEngine;
import com.island.nature.NaturePlugin;
import com.island.nature.config.Configuration;
import com.island.util.DefaultRandomProvider;

public class SimulationBootstrap {

    public SimulationContext<Organism> setup() {
        return setup(Configuration.load());
    }

    public SimulationContext<Organism> setup(Configuration config) {
        com.island.nature.view.ConsoleView view = new com.island.nature.view.ConsoleView();
        NaturePlugin plugin = new NaturePlugin(config, view);
        SimulationEngine<Organism> engine = new SimulationEngine<>();
        
        // Use build() instead of start() so the loop doesn't start a background thread
        return engine.build(plugin, config.getTickDurationMs(), 4, view);
    }
}
