package com.island.nature;

import com.island.engine.GameLoop;
import com.island.engine.SimulationPlugin;
import com.island.engine.SimulationWorld;
import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.entities.TaskRegistry;
import com.island.nature.entities.WorldInitializer;
import com.island.nature.model.Island;
import com.island.nature.service.StatisticsService;
import com.island.nature.view.ConsoleView;
import com.island.util.InteractionMatrix;
import com.island.util.InteractionProvider;

/**
 * Plugin implementation for the Nature (Island) simulation.
 */
public class NaturePlugin implements SimulationPlugin<Organism> {
    private final Configuration config;
    private final SpeciesRegistry speciesRegistry;
    private final InteractionProvider interactionMatrix;
    private final AnimalFactory animalFactory;
    private final StatisticsService statisticsService;
    private final com.island.nature.view.SimulationView view;

    public NaturePlugin(Configuration config) {
        this(config, new ConsoleView());
    }

    public NaturePlugin(Configuration config, com.island.nature.view.SimulationView view) {
        this.config = config;
        this.view = view;
        this.speciesRegistry = new SpeciesLoader(config).load();
        this.interactionMatrix = InteractionMatrix.buildFrom(speciesRegistry);
        this.statisticsService = new StatisticsService(config);
        this.animalFactory = new AnimalFactory(speciesRegistry, new com.island.util.DefaultRandomProvider());
    }

    @Override
    public SimulationWorld<Organism> createWorld() {
        Island island = new Island(config, config.getIslandWidth(), config.getIslandHeight(), 
                                   speciesRegistry, statisticsService);
        
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, speciesRegistry, animalFactory, 
                               java.util.concurrent.Executors.newSingleThreadExecutor(), 
                               new com.island.util.DefaultRandomProvider());
        island.init();
        return island;
    }

    @Override
    public void registerTasks(GameLoop<Organism> gameLoop, SimulationWorld<Organism> world) {
        NatureWorld natureWorld = (NatureWorld) world;
        
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, natureWorld, interactionMatrix, 
                                                     animalFactory, speciesRegistry, view, 
                                                     new com.island.util.DefaultRandomProvider());
        taskRegistry.registerAll();
    }
}
