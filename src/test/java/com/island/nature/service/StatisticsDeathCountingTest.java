package com.island.nature.service;

import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.model.Island;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionMatrix;

class StatisticsDeathCountingTest {
    private Island island;
    private StatisticsService statisticsService;
    private EventBus eventBus;
    private AnimalFactory animalFactory;
    private SpeciesRegistry registry;
    private Configuration config;
    private RandomProvider random;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newCachedThreadPool();
        config = new Configuration();
        random = new DefaultRandomProvider();
        registry = new SpeciesLoader(config).load();
        statisticsService = new StatisticsService(config);
        eventBus = new DefaultEventBus();
        statisticsService.subscribe(eventBus);

        animalFactory = new AnimalFactory(registry, random);
        
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        DefaultProtectionService protectionService = new DefaultProtectionService(config, registry, statisticsService, 1);
        
        NatureDomainContext domainContext = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(matrix)
                .animalFactory(animalFactory)
                .statisticsService(statisticsService)
                .protectionService(protectionService)
                .randomProvider(random)
                .build();
        
        island = new Island(domainContext, 1, 1, eventBus);
        
        DefaultHuntingStrategy huntingStrategy = new DefaultHuntingStrategy(config, matrix);
        
        new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, executor, random);
        new MovementService(island, registry, executor, random);
        new ReproductionService(island, animalFactory, registry, executor, random);
        new CleanupService(island, animalFactory, executor, random);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    void feedingDeathShouldBeReportedOnce() {
        Cell cell = island.getCell(0, 0);
        Animal wolf = animalFactory.createInitialAnimal(new SpeciesKey("wolf", true)).orElseThrow();
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        assertEquals(2, statisticsService.getTotalPopulation());
        
        rabbit.die(DeathCause.EATEN);
        cell.removeAnimal(rabbit);
        
        assertEquals(1, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.EATEN).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }

    @Test
    void packFeedingDeathShouldBeReportedOnce() {
        Cell cell = island.getCell(0, 0);
        Animal wolf1 = animalFactory.createInitialAnimal(new SpeciesKey("wolf", true)).orElseThrow();
        Animal wolf2 = animalFactory.createInitialAnimal(new SpeciesKey("wolf", true)).orElseThrow();
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        
        cell.addAnimal(wolf1);
        cell.addAnimal(wolf2);
        cell.addAnimal(rabbit);
        
        rabbit.die(DeathCause.EATEN_BY_PACK);
        cell.removeAnimal(rabbit);
        
        assertEquals(2, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.EATEN_BY_PACK).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }

    @Test
    void exhaustionDeathShouldBeReportedOnce() {
        Cell cell = island.getCell(0, 0);
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        cell.addAnimal(rabbit);
        
        rabbit.die(DeathCause.MOVEMENT_EXHAUSTION);
        cell.removeAnimal(rabbit);
        
        assertEquals(0, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.MOVEMENT_EXHAUSTION).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }

    @Test
    void reproductionExhaustionShouldBeReportedOnce() {
        Cell cell = island.getCell(0, 0);
        Animal r1 = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        Animal r2 = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        cell.addAnimal(r1);
        cell.addAnimal(r2);

        r1.die(DeathCause.REPRODUCTION_EXHAUSTION);
        cell.removeAnimal(r1);

        assertEquals(1, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.REPRODUCTION_EXHAUSTION).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }

    @Test
    void movementServiceShouldNotDoubleReportDeath() {
        Cell cell = island.getCell(0, 0);
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        cell.addAnimal(rabbit);

        // Manually kill it with MOVEMENT_EXHAUSTION cause
        rabbit.die(DeathCause.MOVEMENT_EXHAUSTION);
        
        assertEquals(true, !rabbit.isAlive());
        assertEquals(DeathCause.MOVEMENT_EXHAUSTION, rabbit.getLastDeathCause());

        CleanupService cleanupService = new CleanupService(island, animalFactory, executor, random);
        
        // Cleanup should remove it and trigger the event exactly once
        cleanupService.processCell(cell, 0);

        assertEquals(0, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.MOVEMENT_EXHAUSTION).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }
}