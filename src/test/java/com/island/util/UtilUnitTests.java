package com.island.util;

import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.model.StaticChunkingStrategy;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;
import com.island.util.common.RandomUtils;
import com.island.util.math.GridUtils;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.model.InteractionMatrix;

class UtilUnitTests {

    @Test
    @DisplayName("RandomUtils: Determinism with Mock Provider")
    void testDeterministicRandom() {
        RandomProvider mockProvider = new RandomProvider() {
            @Override public int nextInt(int bound) { return 42 % bound; }
            @Override public int nextInt(int origin, int bound) { return origin; }
            @Override public double nextDouble() { return 0.5; }
            @Override public double nextDouble(double bound) { return bound / 2.0; }
            @Override public long nextLong() { return 0L; }
            @Override public boolean nextBoolean() { return false; }
        };

        RandomUtils.setProvider(mockProvider);
        try {
            assertEquals(42 % 100, RandomUtils.nextInt(100));
            assertEquals(0.5, RandomUtils.nextDouble());
            assertTrue(RandomUtils.checkChance(60));
            assertFalse(RandomUtils.checkChance(40));
        } finally {
            RandomUtils.setProvider(new DefaultRandomProvider());
        }
    }

    @Test
    @DisplayName("GridUtils: Coordinate Validation")
    void testGridUtilsIsValid() {
        assertTrue(GridUtils.isValid(0, 0, 10, 10));
        assertTrue(GridUtils.isValid(9, 9, 10, 10));
        assertFalse(GridUtils.isValid(-1, 0, 10, 10));
        assertFalse(GridUtils.isValid(0, 10, 10, 10));
    }

    @Test
    @DisplayName("GridUtils: Double Locking Consistency")
    void testDoubleLocking() {
        Configuration config = new Configuration();
        SpeciesRegistry registry = new SpeciesLoader(config).load();
        ComponentRegistry componentRegistry = new ComponentRegistry();
        StatisticsService statisticsService = new StatisticsService(config);
        
        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .randomProvider(new DefaultRandomProvider())
                .componentRegistry(componentRegistry)
                .build();

        Island island = new Island(context, 2, 2, null);
        Cell c1 = island.getCell(0, 0);
        Cell c2 = island.getCell(1, 1);
        
        AtomicInteger executions = new AtomicInteger();
        GridUtils.executeWithDoubleLock(c1, c2, 0, 0, 1, 1, executions::incrementAndGet);
        GridUtils.executeWithDoubleLock(c2, c1, 1, 1, 0, 0, executions::incrementAndGet);
        
        assertEquals(2, executions.get(), "Both actions should execute without deadlock");
    }
}
