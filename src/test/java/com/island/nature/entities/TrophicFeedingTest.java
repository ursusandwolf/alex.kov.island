package com.island.nature.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.FeedingService;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.service.StatisticsService;
import com.island.nature.config.Configuration;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TrophicFeedingTest {
    private Island island;
    private Cell cell;
    private InteractionMatrix matrix;
    private FeedingService feedingService;
    private final Configuration config = new Configuration();
    private final SpeciesRegistry registry = new SpeciesLoader(config).load();

    @BeforeEach
    void setUp() {
        StatisticsService statisticsService = new StatisticsService(config);
        matrix = new InteractionMatrix(registry);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        AnimalFactory animalFactory = new AnimalFactory(registry, randomProvider);

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
        cell = island.getCell(0, 0);
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(config, matrix);
        feedingService = new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), randomProvider, new DefaultEventBus());
    }

    @Test
    void testIntraspeciesPredation() {
        // Wolf eats Wolf (Intraspecies Predation) with 10% chance
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.WOLF, 10);
        
        GenericAnimal predator = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        GenericAnimal victim = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        
        predator.setEnergy(predator.getMaxEnergy() / 2);
        long initialEnergy = predator.getCurrentEnergy();
        
        cell.addAnimal(predator);
        cell.addAnimal(victim);
        
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.WOLF, 100);
        feedingService.tick(1);
        
        assertTrue(predator.getCurrentEnergy() > initialEnergy, "Wolf energy should increase after eating another wolf");
        assertEquals(1, cell.getAnimalCount());
    }

    @Test
    void testInterspeciesPredatorPredation() {
        // Wolf eats Fox
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 30);
        
        for (int i = 0; i < 5; i++) {
            GenericAnimal fox = new GenericAnimal(registry.getAnimalType(SpeciesKey.FOX).orElseThrow());
            cell.addAnimal(fox);
        }
        
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() / 2);
        cell.addAnimal(wolf);
        
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 100);
        feedingService.tick(1);
        
        assertTrue(cell.getAnimalCount() < 6);
        assertTrue(wolf.isAlive());
    }

    @Test
    void testPreyHidingMechanic() {
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow()));
        }
        
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() / 2);
        cell.addAnimal(wolf);
        
        Animal target = (Animal) cell.getAnimals().get(0);
        target.setHiding(true);
        
        feedingService.tick(1);
        
        assertTrue(target.isAlive());
        assertTrue(cell.getAnimalCount() < 11);
    }
}
