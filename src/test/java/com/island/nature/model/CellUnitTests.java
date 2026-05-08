package com.island.nature.model;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.sampling.SamplingContext;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.GenericAnimal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;

class CellUnitTests {
    private Cell cell;
    private EntityContainer container;
    private SpeciesRegistry registry;
    private AnimalFactory factory;
    private ComponentRegistry componentRegistry;
    private Configuration config;

    @BeforeEach
    void setUp() {
        config = new Configuration();
        registry = new SpeciesLoader(config).load();
        componentRegistry = new ComponentRegistry();
        StatisticsService statisticsService = new StatisticsService(config);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        factory = new AnimalFactory(registry, randomProvider, componentRegistry);

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
                .componentRegistry(componentRegistry)
                .build();

        Island island = new Island(context, 1, 1, new DefaultEventBus());
        cell = new Cell(0, 0, island);
        container = cell.getContainer();
    }

    @Test
    @DisplayName("Cell: Basic Add/Remove Animals")
    void testCellAddRemove() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        GenericAnimal wolf = new GenericAnimal(wolfType, componentRegistry);
        
        assertTrue(cell.addAnimal(wolf));
        assertEquals(1, cell.getAnimalCount());
        assertEquals(1, cell.countAnimalsByType(wolfType));
        
        assertTrue(cell.removeAnimal(wolf));
        assertEquals(0, cell.getAnimalCount());
    }

    @Test
    @DisplayName("Cell: Capacity Limits")
    void testCellCapacity() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        int max = wolfType.getMaxPerCell();
        for (int i = 0; i < max; i++) {
            assertTrue(cell.addAnimal(new GenericAnimal(wolfType, componentRegistry)));
        }
        assertFalse(cell.addAnimal(new GenericAnimal(wolfType, componentRegistry)));
    }

    @Test
    @DisplayName("Cell: Dead Organism Cleanup")
    void testCleanupDead() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        GenericAnimal aliveWolf = new GenericAnimal(wolfType, componentRegistry);
        GenericAnimal deadWolf = new GenericAnimal(wolfType, componentRegistry);
        deadWolf.consumeEnergy(deadWolf.getMaxEnergy());
        
        cell.addAnimal(aliveWolf);
        cell.addAnimal(deadWolf);
        
        assertEquals(2, cell.getAnimalCount());
        cell.cleanupDeadEntities(a -> {});
        assertEquals(1, cell.getAnimalCount());
        assertTrue(cell.getAnimals().contains(aliveWolf));
    }

    @Test
    @DisplayName("Container: Maintain Order")
    void testContainerOrder() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        Animal wolf1 = new GenericAnimal(wolfType, componentRegistry);
        Animal wolf2 = new GenericAnimal(wolfType, componentRegistry);
        
        container.addAnimal(wolf1);
        container.addAnimal(wolf2);
        
        var ordered = container.getByType(wolfType);
        assertSame(wolf1, ordered.get(0));
        assertSame(wolf2, ordered.get(1));
    }

    @Test
    @DisplayName("Iteration: Basic and Sampled")
    void testIteration() {
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachAnimal(a -> count.incrementAndGet());
        assertEquals(10, count.get());

        count.set(0);
        SamplingContext context = new SamplingContext(5, new DefaultRandomProvider() {
            @Override public int nextInt(int bound) { return 0; }
        });
        cell.forEachAnimalSampled(context, a -> count.incrementAndGet());
        // size=10, limit=5 -> step=(10/5)+1 = 3. i=0, 3, 6, 9 -> 4 animals
        assertEquals(4, count.get());
    }

    @Test
    @DisplayName("Iteration: Predators and Herbivores")
    void testPredatorHerbivoreIteration() {
        cell.addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
        cell.addAnimal(factory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow());

        AtomicInteger pCount = new AtomicInteger();
        cell.forEachPredator(a -> {
            assertTrue(a.isAnimalPredator());
            pCount.incrementAndGet();
        });
        
        AtomicInteger hCount = new AtomicInteger();
        cell.forEachHerbivoreSampled(100, new DefaultRandomProvider(), a -> {
            assertFalse(a.isAnimalPredator());
            hCount.incrementAndGet();
        });
        
        assertEquals(1, pCount.get());
        assertEquals(1, hCount.get());
    }
}
