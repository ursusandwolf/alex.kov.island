package com.island.nature;

import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Island;
import com.island.nature.view.ConsoleView;
import com.island.nature.view.HeadlessView;
import com.island.nature.view.SimulationView;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.WorldInitializer;
import com.island.nature.entities.domain.NatureDomainContextFactory;

/**
 * Plugin implementation for the Nature (Island) simulation.
 */
public class NaturePlugin implements SimulationPlugin<Organism> {
    private final Configuration config;
    private final NatureDomainContext domainContext;
    private final SimulationView view;
    private final WorldSnapshot initialSnapshot;

    public NaturePlugin() {
        this(Configuration.load(), null);
    }

    public NaturePlugin(Configuration config) {
        this(config, null);
    }

    public NaturePlugin(Configuration config, WorldSnapshot initialSnapshot) {
        this(config, config.isHeadless() ? new HeadlessView() : new ConsoleView(), initialSnapshot);
    }

    public NaturePlugin(Configuration config, SimulationView view, WorldSnapshot initialSnapshot) {
        this.config = config;
        this.view = view;
        this.initialSnapshot = initialSnapshot;
        this.domainContext = NatureDomainContextFactory.create(config);
    }

    public NatureDomainContext getDomainContext() {
        return domainContext;
    }

    @Override
    public SimulationWorld<Organism> createWorld(EventBus eventBus) {
        int width = initialSnapshot != null ? initialSnapshot.getWidth() : config.getIslandWidth();
        int height = initialSnapshot != null ? initialSnapshot.getHeight() : config.getIslandHeight();

        Island island = new Island(domainContext, width, height, eventBus);

        WorldInitializer initializer = new WorldInitializer();
        try (ExecutorService initExecutor = Executors.newSingleThreadExecutor()) {
            if (initialSnapshot != null) {
                initializer.initializeFromSnapshot(island, domainContext.getSpeciesRegistry(), domainContext.getAnimalFactory(), 
                                                   initialSnapshot, initExecutor, domainContext.getRandomProvider());
            } else {
                initializer.initialize(island, domainContext.getSpeciesRegistry(), domainContext.getAnimalFactory(), 
                                       initExecutor, domainContext.getRandomProvider());
            }
        }
        island.init();
        island.rebalance(); // Ensure partition reflects initial population
        return island;
    }

    @Override
    public void registerTasks(GameLoop<Organism> gameLoop, SimulationWorld<Organism> world, EventBus eventBus) {
        NatureWorld natureWorld = (NatureWorld) world;

        TaskRegistry taskRegistry = new TaskRegistry(gameLoop, natureWorld, domainContext, domainContext.getInteractionProvider(), 
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