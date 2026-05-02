package com.island.nature;

import com.island.engine.GameLoop;
import com.island.engine.SimulationPlugin;
import com.island.engine.SimulationWorld;
import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.entities.TaskRegistry;
import com.island.nature.entities.WorldInitializer;
import com.island.nature.model.Island;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.entities.BiomassManager;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.nature.view.ConsoleView;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;

/**
 * Plugin implementation for the Nature (Island) simulation.
 */
public class NaturePlugin implements SimulationPlugin<Organism> {
    private final Configuration config;
    private final NatureDomainContext domainContext;
    private final com.island.nature.view.SimulationView view;

    public NaturePlugin(Configuration config) {
        this(config, new ConsoleView());
    }

    public NaturePlugin(Configuration config, com.island.nature.view.SimulationView view) {
        this.config = config;
        this.view = view;
        
        SpeciesRegistry speciesRegistry = new SpeciesLoader(config).load();
        InteractionProvider interactionMatrix = InteractionMatrix.buildFrom(speciesRegistry);
        StatisticsService statisticsService = new StatisticsService(config);
        RandomProvider randomProvider = new DefaultRandomProvider();
        AnimalFactory animalFactory = new AnimalFactory(speciesRegistry, randomProvider);
        ProtectionService protectionService = new DefaultProtectionService(config, speciesRegistry, 
                                                                          statisticsService, 
                                                                          config.getIslandWidth() * config.getIslandHeight());
        BiomassManager biomassManager = new DefaultBiomassManager();

        this.domainContext = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(speciesRegistry)
                .interactionProvider(interactionMatrix)
                .statisticsService(statisticsService)
                .animalFactory(animalFactory)
                .protectionService(protectionService)
                .biomassManager(biomassManager)
                .randomProvider(randomProvider)
                .build();
    }

    @Override
    public SimulationWorld<Organism> createWorld() {
        Island island = new Island(domainContext, config.getIslandWidth(), config.getIslandHeight());
        
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, domainContext.getSpeciesRegistry(), domainContext.getAnimalFactory(), 
                               java.util.concurrent.Executors.newSingleThreadExecutor(), 
                               domainContext.getRandomProvider());
        island.init();
        return island;
    }

    @Override
    public void registerTasks(GameLoop<Organism> gameLoop, SimulationWorld<Organism> world) {
        NatureWorld natureWorld = (NatureWorld) world;
        
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, natureWorld, domainContext.getInteractionProvider(), 
                                                     domainContext.getAnimalFactory(), domainContext.getSpeciesRegistry(), 
                                                     view, domainContext.getRandomProvider());
        taskRegistry.registerAll();
    }
}
