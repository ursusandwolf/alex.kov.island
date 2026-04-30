package com.island.model;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CellIterationTest {
    private Cell cell;
    private SpeciesRegistry registry;
    private AnimalFactory factory;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        Island island = new Island(1, 1, registry, new StatisticsService());
        cell = new Cell(0, 0, island);
        factory = new AnimalFactory(registry, new DefaultRandomProvider());
    }

    @Test
    @DisplayName("forEachAnimal should iterate over all animals")
    void testForEachAnimal() {
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(factory.createAnimal(SpeciesKey.WOLF).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachAnimal(a -> count.incrementAndGet());
        assertEquals(10, count.get());
    }

    @Test
    @DisplayName("forEachAnimalSampled should respect limit and be deterministic for 0 random")
    void testForEachAnimalSampled() {
        for (int i = 0; i < 100; i++) {
            cell.addAnimal(factory.createAnimal(SpeciesKey.MOUSE).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        // Limit 10, step should be 100/10 + 1 = 11. Actually size/limit + 1. 100/10 + 1 = 11.
        // Wait, 100 animals, limit 10. step = 100/10 + 1 = 11.
        // i=0, 11, 22, 33, 44, 55, 66, 77, 88, 99. Total 10.
        cell.forEachAnimalSampled(10, new DefaultRandomProvider() {
            @Override
            public int nextInt(int bound) { return 0; }
        }, a -> count.incrementAndGet());
        
        assertEquals(10, count.get());
    }

    @Test
    @DisplayName("forEachHerbivoreSampled should only iterate over herbivores")
    void testForEachHerbivoreSampled() {
        for (int i = 0; i < 5; i++) {
            cell.addAnimal(factory.createAnimal(SpeciesKey.WOLF).orElseThrow());
            cell.addAnimal(factory.createAnimal(SpeciesKey.RABBIT).orElseThrow());
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
            cell.addAnimal(factory.createAnimal(SpeciesKey.WOLF).orElseThrow());
            cell.addAnimal(factory.createAnimal(SpeciesKey.RABBIT).orElseThrow());
        }

        AtomicInteger count = new AtomicInteger();
        cell.forEachPredator(a -> {
            assertTrue(a.isAnimalPredator());
            count.incrementAndGet();
        });
        
        assertEquals(5, count.get());
    }
}
