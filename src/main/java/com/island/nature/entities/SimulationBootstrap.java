package com.island.nature.entities;

import com.island.nature.config.ConfigLoader;
import com.island.nature.config.Configuration;
import com.island.engine.GameLoop;
import com.island.engine.SimulationContext;
import com.island.nature.model.Island;
import com.island.nature.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import com.island.nature.view.ConsoleView;
import com.island.nature.view.SimulationView;

public class SimulationBootstrap {
    private final ConfigLoader configLoader = new ConfigLoader();

    public SimulationContext<Organism> setup() {
        return setup(configLoader.loadGeneralConfig());
    }

    public SimulationContext<Organism> setup(Configuration config) {
        SpeciesRegistry registry = new SpeciesLoader().load();
        StatisticsService statisticsService = new StatisticsService();
        RandomProvider random = new DefaultRandomProvider();

        Island island = new Island(config.getIslandWidth(), config.getIslandHeight(), registry, statisticsService);
        AnimalFactory animalFactory = new AnimalFactory(registry, random);

        int processors = Runtime.getRuntime().availableProcessors();
        int totalCells = config.getIslandWidth() * config.getIslandHeight();
        int threadCount = Math.min(processors, Math.max(1, totalCells / 4));
        if (totalCells >= 64 && threadCount < 4) {
            threadCount = 4;
        }

        GameLoop<Organism> gameLoop = new GameLoop<>(config.getTickDurationMs(), threadCount);
        gameLoop.setWorld(island);

        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, registry, animalFactory, gameLoop.getTaskExecutor(), random);
        island.init();

        InteractionMatrix matrix = configLoader.loadInteractionMatrix(registry);
        SimulationView view = new ConsoleView();
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, island, matrix, animalFactory, registry, view, random);
        taskRegistry.registerAll();

        return new SimulationContext<>(island, gameLoop, view, random);
    }
}
