package com.island.nature.service;

import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.DefaultHuntingStrategy;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.nature.model.Island;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import java.util.Collections;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsDeathCountingTest {
    private Island island;
    private StatisticsService statisticsService;
    private FeedingService feedingService;
    private MovementService movementService;
    private ReproductionService reproductionService;
    private CleanupService cleanupService;
    private EventBus eventBus;
    private AnimalFactory animalFactory;
    private SpeciesRegistry registry;
    private Configuration config;
    private RandomProvider random;

    @BeforeEach
    void setUp() {
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
        
        feedingService = new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), random, eventBus);
        movementService = new MovementService(island, registry, Executors.newSingleThreadExecutor(), random, eventBus);
        reproductionService = new ReproductionService(island, animalFactory, registry, Executors.newSingleThreadExecutor(), random, eventBus);
        cleanupService = new CleanupService(island, animalFactory, Executors.newSingleThreadExecutor(), random);
    }

    @Test
    void feedingDeathShouldBeReportedOnce() {
        Cell cell = island.getCell(0, 0);
        Animal wolf = animalFactory.createInitialAnimal(new SpeciesKey("wolf", true)).orElseThrow();
        Animal rabbit = animalFactory.createInitialAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        // Initial state: 2 births recorded
        assertEquals(2, statisticsService.getTotalPopulation());
        
        // Wolf eats rabbit
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
        
        // Die from exhaustion
        rabbit.die(DeathCause.MOVEMENT_EXHAUSTION);
        cell.removeAnimal(rabbit);
        
        assertEquals(0, statisticsService.getTotalPopulation());
        assertEquals(1, statisticsService.getTotalDeaths(DeathCause.MOVEMENT_EXHAUSTION).getOrDefault(new SpeciesKey("rabbit", false), 0));
    }
}
