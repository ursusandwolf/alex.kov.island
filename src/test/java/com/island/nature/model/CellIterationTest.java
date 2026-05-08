package com.island.nature.model;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.StatisticsService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.common.DefaultRandomProvider;
import com.island.nature.model.InteractionMatrix;
import com.island.util.sampling.SamplingContext;

class CellIterationTest {
    private Cell cell;
    private SpeciesRegistry registry;
    private AnimalFactory factory;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        registry = new SpeciesLoader(config).load();
        StatisticsService statisticsService = new StatisticsService(config);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        factory = new AnimalFactory(registry, randomProvider, new ComponentRegistry());

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .animalFactory(factory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .randomProvider(randomProvider)
                .build();

        Island island = new Island(context, 1, 1, new DefaultEventBus());
        cell = new Cell(0, 0, island);
    }

    @Test
    @DisplayName("forEachAnimal should iterate over all animals")
    void testForEachAnimal() {
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachAnimal(a -> count.incrementAndGet());
        assertEquals(10, count.get());
        }

        @Test
        @DisplayName("forEachAnimalSampled should respect limit and be deterministic for 0 random")
        void testForEachAnimalSampled() {
        for (int i = 0; i < 100; i++) {
            cell.addAnimal(factory.createAnimal(new SpeciesKey("mouse", false)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        // Limit 10, step should be 100/10 + 1 = 11. Actually size/limit + 1. 100/10 + 1 = 11.
        // i=0, 11, 22, 33, 44, 55, 66, 77, 88, 99. Total 10.
        cell.forEachAnimalSampled(new SamplingContext(10, new DefaultRandomProvider() {
            @Override
            public int nextInt(int bound) { return 0; }
        }), a -> count.incrementAndGet());

        assertEquals(10, count.get());
    }

    @Test
    @DisplayName("forEachHerbivoreSampled should only iterate over herbivores")
    void testForEachHerbivoreSampled() {
        for (int i = 0; i < 5; i++) {
            cell.addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
            cell.addAnimal(factory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachHerbivoreSampled(100, new DefaultRandomProvider(), a -> {
            assertFalse(a.isAnimalPredator());
            count.incrementAndGet();
        });
        
        assertEquals(5, count.get());
    }

    @Test
    @DisplayName("forEachPredator should only iterate over predators")
    void testForEachPredator() {
        for (int i = 0; i < 5; i++) {
            cell.addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
            cell.addAnimal(factory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachPredator(a -> {
            assertTrue(a.isAnimalPredator());
            count.incrementAndGet();
        });
        
        assertEquals(5, count.get());
    }
}