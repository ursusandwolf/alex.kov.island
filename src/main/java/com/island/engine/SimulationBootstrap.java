package com.island.engine;

import com.island.config.ConfigLoader;
import com.island.config.Configuration;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.view.ConsoleView;

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
        // 1. Load configuration
        SpeciesConfig speciesConfig = SpeciesConfig.getInstance();
        
        // 2. Setup interaction matrix
        InteractionMatrix matrix = configLoader.loadInteractionMatrix(speciesConfig);

        // 3. Create core models
        Island island = new Island(config.getIslandWidth(), config.getIslandHeight());
        AnimalFactory animalFactory = new AnimalFactory(speciesConfig);

        // 4. Setup GameLoop and View
        GameLoop gameLoop = new GameLoop(config.getTickDurationMs());
        ConsoleView consoleView = new ConsoleView();

        // 5. Initialize world population
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, speciesConfig, animalFactory, gameLoop.getTaskExecutor());

        // 6. Register simulation tasks
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, island, matrix, animalFactory, speciesConfig, consoleView);
        taskRegistry.registerAll();

        return new SimulationContext(island, gameLoop, speciesConfig, consoleView);
    }
}
