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
import com.island.nature.model.StaticChunkingStrategy;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.interaction.InteractionMatrix;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;

class AnimalHealthSystemTest {
    private AnimalHealthSystem healthSystem;
    private Island island;
    private Cell cell;
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
                .protectionService(new DefaultProtectionService(config, registry, stats, 1))
                .biomassManager(new DefaultBiomassManager())
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .randomProvider(random)
                .componentRegistry(componentRegistry)
                .build();

        island = new Island(domainContext, 1, 1, new DefaultEventBus());
        cell = island.getCell(0, 0);
        healthSystem = new AnimalHealthSystem(island, executor, random);
    }


    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    @DisplayName("Animal should consume energy and eventually die from hunger")
    void animal_should_die_from_hunger() {
        SpeciesKey wolfKey = new SpeciesKey("wolf", true);
        Animal wolf = domainContext.getAnimalFactory().createAnimal(wolfKey).orElseThrow();
        cell.addAnimal(wolf);
        
        long initialEnergy = wolf.getCurrentEnergy();
        assertTrue(initialEnergy > 0);
        
        healthSystem.processCell(cell, 1);
        
        assertTrue(wolf.getCurrentEnergy() < initialEnergy, "Energy should decrease after health system process");
        
        // Rapidly advance time/hunger
        int safetyBreak = 0;
        while(wolf.isAlive() && safetyBreak < 1000) {
            healthSystem.processCell(cell, 1);
            safetyBreak++;
        }
        
        assertFalse(wolf.isAlive(), "Wolf should be dead after multiple ticks without food");
    }
}
