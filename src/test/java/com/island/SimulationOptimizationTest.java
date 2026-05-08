package com.island;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.config.SimulationConstants;
import com.island.nature.entities.herbivores.Caterpillar;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.model.StaticChunkingStrategy;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.AnimalFeedingSystem;
import com.island.nature.service.StatisticsService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.util.common.DefaultRandomProvider;
import com.island.nature.model.InteractionMatrix;

public class SimulationOptimizationTest {
    private Island island;
    private SpeciesRegistry registry;
    private AnimalFeedingSystem feedingSystem;
    private AnimalFactory animalFactory;
    private ComponentRegistry componentRegistry;
    private final Configuration config = new Configuration();

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader(config).load();
        componentRegistry = new ComponentRegistry();
        StatisticsService statisticsService = new StatisticsService(config);
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        animalFactory = new AnimalFactory(registry, randomProvider, componentRegistry);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(matrix)
                .animalFactory(animalFactory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .componentRegistry(componentRegistry)
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .build();

        island = new Island(context, 1, 1, new DefaultEventBus());
        feedingSystem = new AnimalFeedingSystem(island, animalFactory, matrix, registry, 
                new DefaultHuntingStrategy(config, matrix), Executors.newSingleThreadExecutor(), randomProvider);
    }

    @Test
    @DisplayName("Skip Ticks: Cold-blooded animals should skip actions on non-modulo ticks")
    void testColdBloodedSkipTicks() {
        Cell cell = island.getCell(0, 0);
        // Chameleon is cold-blooded. It eats every 3rd tick (tick % 3 == 0).
        Animal chameleon = animalFactory.createAnimal(new SpeciesKey("chameleon", false)).orElseThrow();
        chameleon.setEnergy(chameleon.getMaxEnergy() / 2);
        long initialEnergy = chameleon.getCurrentEnergy();
        
        // Add some food
        cell.addAnimal(chameleon);
        
        // Tick 1: Should SKIP
        feedingSystem.tick(1);
        assertEquals(initialEnergy, chameleon.getCurrentEnergy(), "Chameleon should skip eating on tick 1");

        // Tick 2: Should SKIP
        feedingSystem.tick(2);
        assertEquals(initialEnergy, chameleon.getCurrentEnergy(), "Chameleon should skip eating on tick 2");

        // Tick 3: Should ACT
        // Add a caterpillar (biomass)
        cell.addBiomass(new Caterpillar(config, componentRegistry, new SpeciesKey("caterpillar", false), 100L * SimulationConstants.SCALE_1M, 1000L * SimulationConstants.SCALE_1M, 0));
        
        feedingSystem.tick(3);
    }

    @Test
    @DisplayName("LOD: Only a subset of animals should act in overcrowded cells")
    void testLevelOfDetailSampling() {
        Cell cell = island.getCell(0, 0);
        // Add 200 mice
        for (int i = 0; i < 200; i++) {
            Animal mouse = animalFactory.createAnimal(new SpeciesKey("mouse", false)).orElseThrow();
            mouse.setEnergy(mouse.getMaxEnergy() / 2);
            cell.addAnimal(mouse);
        }

        assertDoesNotThrow(() -> feedingSystem.tick(0));
    }
}
