package com.island.engine;

import com.island.config.ConfigLoader;
import com.island.config.Configuration;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import com.island.view.ConsoleView;
import com.island.view.SimulationView;

/**
 * Responsible for initializing all components of the simulation.
 * Adheres to SRP by delegating to specialized loaders and registries.
 */
public class SimulationBootstrap {
    private final ConfigLoader configLoader = new ConfigLoader();

    public SimulationContext setup() {
        return setup(configLoader.loadGeneralConfig());
    }

    public SimulationContext setup(Configuration config) {
        // 1. Load configuration and species registry
        SpeciesRegistry registry = new SpeciesLoader().load();
        
        // 2. Setup interaction matrix and random provider
        InteractionMatrix matrix = configLoader.loadInteractionMatrix(registry);
        RandomProvider random = new DefaultRandomProvider();

        // 3. Create core models
        Island island = new Island(config.getIslandWidth(), config.getIslandHeight(), registry);
        AnimalFactory animalFactory = new AnimalFactory(registry, random);

        // 4. Setup GameLoop and View
        SimulationView view = new ConsoleView();
        GameLoop gameLoop = new GameLoop(config.getTickDurationMs());

        // 5. Initialize world population
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, registry, animalFactory, gameLoop.getTaskExecutor(), random);
        island.init();

        // 6. Register simulation tasks
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, island, matrix, animalFactory, registry, view, random);
        taskRegistry.registerAll();

        return new SimulationContext(island, gameLoop, registry, view, random);
    }
}
