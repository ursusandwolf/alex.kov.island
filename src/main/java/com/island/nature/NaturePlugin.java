package com.island.nature;

import com.island.engine.event.EventBus;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.AlertService;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.nature.view.ConsoleView;
import com.island.nature.view.SimulationView;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.registry.WorldInitializer;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionMatrix;
import com.island.util.interaction.InteractionProvider;

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
        ComponentRegistry componentRegistry = new ComponentRegistry();
        AnimalFactory animalFactory = new AnimalFactory(speciesRegistry, randomProvider, componentRegistry);
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
                .componentRegistry(componentRegistry)
                .build();
    }

    @Override
    public SimulationWorld<Organism> createWorld(EventBus eventBus) {
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
    public void registerTasks(GameLoop<Organism> gameLoop, SimulationWorld<Organism> world, EventBus eventBus) {
        NatureWorld natureWorld = (NatureWorld) world;
        
        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, natureWorld, domainContext.getInteractionProvider(), 
                                                     domainContext.getAnimalFactory(), domainContext.getSpeciesRegistry(), 
                                                     view, domainContext.getRandomProvider(), eventBus);
        taskRegistry.registerAll();
    }

    @Override
    public void onSimulationStarted(SimulationContext<Organism> context) {
        domainContext.getStatisticsService().subscribe(context.eventBus());
        domainContext.getAlertService().subscribe(context.eventBus());
    }

    @Override
    public boolean shouldStop(SimulationContext<Organism> context) {
        if (!(context.world() instanceof Island island)) {
            return false;
        }

        Map<SpeciesKey, Integer> counts = island.getSpeciesCounts();
        int totalAnimals = 0;
        for (SpeciesKey species : island.getRegistry().getAllAnimalKeys()) {
            boolean isBiomass = island.getRegistry().getAnimalType(species)
                    .map(AnimalType::isBiomass).orElse(false);
            if (!isBiomass) {
                totalAnimals += counts.getOrDefault(species, 0);
            }
        }
        return totalAnimals == 0;
    }
}