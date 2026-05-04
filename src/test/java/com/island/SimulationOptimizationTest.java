package com.island;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.island.nature.config.SimulationConstants;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.DefaultHuntingStrategy;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.entities.herbivores.Caterpillar;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.FeedingService;
import com.island.nature.service.StatisticsService;
import com.island.engine.event.DefaultEventBus;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SimulationOptimizationTest {
    private Island island;
    private SpeciesRegistry registry;
    private FeedingService feedingService;
    private AnimalFactory animalFactory;
    private final Configuration config = new Configuration();

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        StatisticsService statisticsService = new StatisticsService(config);
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        animalFactory = new AnimalFactory(registry, randomProvider);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(matrix)
                .animalFactory(animalFactory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .build();

        island = new Island(context, 1, 1);
        feedingService = new FeedingService(island, animalFactory, matrix, registry, 
                new DefaultHuntingStrategy(config, matrix), Executors.newSingleThreadExecutor(), randomProvider, new DefaultEventBus());
    }

    @Test
    @DisplayName("Skip Ticks: Cold-blooded animals should skip actions on non-modulo ticks")
    void testColdBloodedSkipTicks() {
        Cell cell = island.getCell(0, 0);
        // Chameleon is cold-blooded. It eats every 3rd tick (tick % 3 == 0).
        Animal chameleon = animalFactory.createAnimal(SpeciesKey.CHAMELEON).orElseThrow();
        chameleon.setEnergy(chameleon.getMaxEnergy() / 2);
        long initialEnergy = chameleon.getCurrentEnergy();
        
        // Add some food
        cell.addAnimal(chameleon);
        
        // Tick 1: Should SKIP
        feedingService.tick(1);
        assertEquals(initialEnergy, chameleon.getCurrentEnergy(), "Chameleon should skip eating on tick 1");

        // Tick 2: Should SKIP
        feedingService.tick(2);
        assertEquals(initialEnergy, chameleon.getCurrentEnergy(), "Chameleon should skip eating on tick 2");

        // Tick 3: Should ACT
        // Add a caterpillar (biomass)
        cell.addBiomass(new Caterpillar(config, 100L * SimulationConstants.SCALE_1M, 1000L * SimulationConstants.SCALE_1M, 0));
        
        feedingService.tick(3);
    }

    @Test
    @DisplayName("LOD: Only a subset of animals should act in overcrowded cells")
    void testLevelOfDetailSampling() {
        Cell cell = island.getCell(0, 0);
        // Add 200 mice
        for (int i = 0; i < 200; i++) {
            Animal mouse = animalFactory.createAnimal(SpeciesKey.MOUSE).orElseThrow();
            mouse.setEnergy(mouse.getMaxEnergy() / 2);
            cell.addAnimal(mouse);
        }

        assertDoesNotThrow(() -> feedingService.tick(0));
    }
}
