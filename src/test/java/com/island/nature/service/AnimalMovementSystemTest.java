package com.island.nature.service;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.interaction.InteractionMatrix;
import com.island.util.math.GridUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;

class AnimalMovementSystemTest {
    private AnimalMovementSystem movementSystem;
    private Island island;
    private NatureDomainContext domainContext;
    private final Configuration config = new Configuration();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final DefaultRandomProvider random = new DefaultRandomProvider();

    @BeforeEach
    void setUp() {
        SpeciesRegistry registry = new SpeciesLoader(config).load();
        ComponentRegistry componentRegistry = new ComponentRegistry();
        AnimalFactory animalFactory = new AnimalFactory(registry, random, componentRegistry);
        StatisticsService stats = new StatisticsService(config);

        domainContext = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .statisticsService(stats)
                .alertService(new AlertService())
                .animalFactory(animalFactory)
                .protectionService(new DefaultProtectionService(config, registry, stats, 4))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(random)
                .componentRegistry(componentRegistry)
                .build();

        island = new Island(domainContext, 2, 2, new DefaultEventBus());
        movementSystem = new AnimalMovementSystem(island, executor, random);

        
        // Connect cells
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                Cell cell = island.getCell(x, y);
                cell.setNeighbors(GridUtils.getNeighbors(island, cell, 2, 2));
            }
        }
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    void animal_should_move_to_neighbor_cell() {
        SpeciesKey wolfKey = new SpeciesKey("wolf", true);
        Animal wolf = domainContext.getAnimalFactory().createAnimal(wolfKey).orElseThrow();
        Cell startCell = island.getCell(0, 0);
        startCell.addAnimal(wolf);
        
        assertEquals(1, startCell.getAnimalCount());
        
        // We might need to run multiple times because movement depends on tick interval and random
        boolean moved = false;
        for (int i = 0; i < 20; i++) {
            movementSystem.processCell(startCell, i);
            if (startCell.getAnimalCount() == 0) {
                moved = true;
                break;
            }
        }
        
        assertTrue(moved, "Animal should have moved out of the start cell");
        
        int totalAnimals = 0;
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                totalAnimals += island.getCell(x, y).getAnimalCount();
            }
        }
        assertEquals(1, totalAnimals, "Total animal count should remain 1 after movement");
    }
}
