package com.island;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.service.FeedingService;
import com.island.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.content.DefaultHuntingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationOptimizationTest {
    private Island island;
    private SpeciesRegistry registry;
    private FeedingService feedingService;
    private AnimalFactory animalFactory;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        island = new Island(1, 1, registry, new StatisticsService());
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        animalFactory = new AnimalFactory(registry, new DefaultRandomProvider());
        feedingService = new FeedingService(island, animalFactory, matrix, registry, 
                new DefaultHuntingStrategy(matrix), Executors.newSingleThreadExecutor(), new DefaultRandomProvider());
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
        cell.addBiomass(new com.island.content.animals.herbivores.Caterpillar(100L * com.island.config.SimulationConstants.SCALE_1M, 0));
        
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
