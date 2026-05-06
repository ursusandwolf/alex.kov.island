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
import com.island.nature.service.AlertService;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.nature.view.ConsoleView;
import com.island.nature.view.SimulationView;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.AnimalType;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Plugin implementation for the Nature (Island) simulation.
 */
public class NaturePlugin implements SimulationPlugin<Organism> {
    private final Configuration config;
    private final NatureDomainContext domainContext;
    private final SimulationView view;

    public NaturePlugin(Configuration config) {
        this(config, new ConsoleView());
    }

    public NaturePlugin(Configuration config, SimulationView view) {
        this.config = config;
        this.view = view;
        
        SpeciesRegistry speciesRegistry = new SpeciesLoader(config).load();
        InteractionProvider interactionMatrix = InteractionMatrix.buildFrom(speciesRegistry);
        StatisticsService statisticsService = new StatisticsService(config);
        AlertService alertService = new AlertService();
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
                .alertService(alertService)
                .animalFactory(animalFactory)
                .protectionService(protectionService)
                .biomassManager(biomassManager)
                .randomProvider(randomProvider)
                .build();
    }

    @Override
    public SimulationWorld<Organism> createWorld(com.island.engine.event.EventBus eventBus) {
        Island island = new Island(domainContext, config.getIslandWidth(), config.getIslandHeight(), eventBus);
        
        WorldInitializer initializer = new WorldInitializer();
        ExecutorService initExecutor = Executors.newSingleThreadExecutor();
        try {
            initializer.initialize(island, domainContext.getSpeciesRegistry(), domainContext.getAnimalFactory(), 
                                   initExecutor, 
                                   domainContext.getRandomProvider());
        } finally {
            initExecutor.shutdown();
        }
        island.init();
        return island;
    }

    @Override
    public void registerTasks(GameLoop<Organism> gameLoop, SimulationWorld<Organism> world, com.island.engine.event.EventBus eventBus) {
        NatureWorld natureWorld = (NatureWorld) world;
        
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, natureWorld, domainContext.getInteractionProvider(), 
                                                     domainContext.getAnimalFactory(), domainContext.getSpeciesRegistry(), 
                                                     view, domainContext.getRandomProvider(), eventBus);
        taskRegistry.registerAll();
    }

    @Override
    public void onSimulationStarted(com.island.engine.SimulationContext<Organism> context) {
        domainContext.getStatisticsService().subscribe(context.eventBus());
        domainContext.getAlertService().subscribe(context.eventBus());
    }

    @Override
    public boolean shouldStop(com.island.engine.SimulationContext<Organism> context) {
        if (!(context.world() instanceof Island island)) {
            return false;
        }

        Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
        for (SpeciesKey species : island.getRegistry().getAllAnimalKeys()) {
            boolean isBiomass = island.getRegistry().getAnimalType(species)
                    .map(AnimalType::isBiomass).orElse(false);
            if (isBiomass) {
                continue;
            }
            if (counts.getOrDefault(species, 0) == 0) {
                return true;
            }
        }
        return false;
    }
}
