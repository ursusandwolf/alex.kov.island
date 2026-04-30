package com.island.engine;

import com.island.config.ConfigLoader;
import com.island.config.Configuration;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import com.island.view.ConsoleView;
import com.island.view.SimulationView;

public class SimulationBootstrap {
    private final ConfigLoader configLoader = new ConfigLoader();

    public SimulationContext setup() {
        return setup(configLoader.loadGeneralConfig());
    }

    public SimulationContext setup(Configuration config) {
        SpeciesRegistry registry = new SpeciesLoader().load();
        InteractionMatrix matrix = configLoader.loadInteractionMatrix(registry);
        StatisticsService statisticsService = new StatisticsService();
        RandomProvider random = new DefaultRandomProvider();

        Island island = new Island(config.getIslandWidth(), config.getIslandHeight(), registry, statisticsService);
        AnimalFactory animalFactory = new AnimalFactory(registry, random);

        SimulationView view = new ConsoleView();
        GameLoop gameLoop = new GameLoop(config.getTickDurationMs());

        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, registry, animalFactory, gameLoop.getTaskExecutor(), random);
        island.init();

        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, island, matrix, animalFactory, registry, view, random);
        taskRegistry.registerAll();

        return new SimulationContext(island, gameLoop, registry, view, random);
    }
}
