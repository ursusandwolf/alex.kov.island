package com.island.nature;

import com.island.engine.core.NamedSimulationPlugin;
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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Plugin implementation for the Nature (Island) simulation.
 */
@Component
@Getter
@Slf4j
public class NaturePlugin implements NamedSimulationPlugin<Organism> {
    private Configuration config;
    private NatureDomainContext domainContext;
    private SimulationView view;
    private WorldSnapshot initialSnapshot;

    public NaturePlugin() {
        // Default constructor for JPMS and Spring
    }

    public NaturePlugin(Configuration config) {
        this(config, null);
    }

    public NaturePlugin(Configuration config, WorldSnapshot initialSnapshot) {
        this(config, config.isHeadless() ? new HeadlessView() : new ConsoleView(), initialSnapshot);
    }

    public NaturePlugin(Configuration config, SimulationView view, WorldSnapshot initialSnapshot) {
        this.config = config != null ? config : Configuration.load();
        this.view = view != null ? view : (this.config.isHeadless() ? new HeadlessView() : new ConsoleView());
        this.initialSnapshot = initialSnapshot;
        this.domainContext = NatureDomainContextFactory.create(this.config);
    }

    @Override
    public String getPluginName() {
        return "nature";
    }

    @Override
    public SimulationPlugin<Organism> withConfiguration(int width, int height, WorldSnapshot snapshot) {
        Configuration newConfig = Configuration.load(); // Or clone existing if needed
        newConfig.setIslandWidth(width);
        newConfig.setIslandHeight(height);
        return new NaturePlugin(newConfig, view, snapshot);
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
