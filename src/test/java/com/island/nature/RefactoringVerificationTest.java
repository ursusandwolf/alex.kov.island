package com.island.nature;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.model.Island;
import com.island.nature.model.StaticChunkingStrategy;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.engine.core.SimulationNode;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.interaction.InteractionMatrix;
import com.island.util.math.GridUtils;
import com.island.util.sampling.SamplingContext;

class RefactoringVerificationTest {

    private Island island;
    private StatisticsService statisticsService;
    private SpeciesRegistry registry;
    private AnimalFactory animalFactory;
    private ComponentRegistry componentRegistry;
    private Configuration config;

    @BeforeEach
    void setUp() {
        config = new Configuration();
        registry = new SpeciesLoader(config).load();
        componentRegistry = new ComponentRegistry();
        statisticsService = new StatisticsService(config);
        
        // Area = 100 for a 10x10 world
        ProtectionService protectionService = new DefaultProtectionService(config, registry, statisticsService, 100);
        
        InteractionMatrix matrix = new InteractionMatrix(registry);
        animalFactory = new AnimalFactory(registry, new DefaultRandomProvider(), componentRegistry);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .statisticsService(statisticsService)
                .protectionService(protectionService)
                .animalFactory(animalFactory)
                .componentRegistry(componentRegistry)
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .build();

        EventBus bus = new DefaultEventBus();
        island = new Island(context, 10, 10, bus);
        statisticsService.subscribe(bus);
    }

    @Test
    @DisplayName("GridUtils should correctly identify valid coordinates")
    void testGridUtilsIsValid() {
        assertTrue(GridUtils.isValid(0, 0, 10, 10));
        assertTrue(GridUtils.isValid(9, 9, 10, 10));
        assertFalse(GridUtils.isValid(-1, 0, 10, 10));
        assertFalse(GridUtils.isValid(0, 10, 10, 10));
    }

    @Test
    @DisplayName("SamplingContext should be respected by Cell")
    void testSamplingContextInCell() {
        Cell cell = island.getCell(0, 0);
        for (int i = 0; i < 20; i++) {
            cell.addAnimal(animalFactory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        // size=20, limit=5 -> step = 20/5 + 1 = 5. Hits: 0, 5, 10, 15 -> 4 animals.
        SamplingContext context = new SamplingContext(5, new DefaultRandomProvider());
        cell.forEachAnimalSampled(context, a -> count.incrementAndGet());

        assertEquals(4, count.get(), "Should respect the sampling logic with limit 5");
    }

    @Test
    @DisplayName("Centralized death reporting should work when animal dies")
    void testCentralizedDeathReporting() {
        Cell cell = island.getCell(1, 1);
        Animal rabbit = animalFactory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow();
        cell.addAnimal(rabbit);

        int initialCount = statisticsService.getSpeciesCount(new SpeciesKey("rabbit", false));
        assertTrue(initialCount > 0);

        // Simulate death with cause
        rabbit.die(DeathCause.HUNGER);
        
        // Remove from cell - this should trigger the listener in Island
        cell.removeEntity(rabbit);

        assertEquals(initialCount - 1, statisticsService.getSpeciesCount(new SpeciesKey("rabbit", false)), "Population should decrease");
        
        // Verify death stats
        int hungerDeaths = statisticsService.getTotalDeaths(DeathCause.HUNGER).getOrDefault(new SpeciesKey("rabbit", false), 0);
        assertEquals(1, hungerDeaths, "Death should be reported with cause HUNGER");
    }

    @Test
    @DisplayName("Double locking should be consistent in GridUtils")
    void testDoubleLocking() {
        Cell c1 = island.getCell(0, 0);
        Cell c2 = island.getCell(1, 1);
        
        AtomicInteger executions = new AtomicInteger();
        GridUtils.executeWithDoubleLock(c1, c2, 0, 0, 1, 1, executions::incrementAndGet);
        GridUtils.executeWithDoubleLock(c2, c1, 1, 1, 0, 0, executions::incrementAndGet);
        
        assertEquals(2, executions.get(), "Both actions should execute without deadlock");
    }
}
