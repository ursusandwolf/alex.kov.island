package com.island.nature.entities;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.FeedingService;
import com.island.nature.service.StatisticsService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.GenericAnimal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.nature.entities.strategy.HuntingStrategy;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.interaction.InteractionMatrix;

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

        island = new Island(context, 1, 1, new DefaultEventBus());
        cell = island.getCell(0, 0);
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(config, matrix);
        feedingService = new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, executor, randomProvider);
    }

    @Test
    void testIntraspeciesPredation() {
        // Wolf eats Wolf (Intraspecies Predation) with 10% chance
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("wolf", true), 10);
        
        GenericAnimal predator = new GenericAnimal(registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow());
        GenericAnimal victim = new GenericAnimal(registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow());
        
        predator.setEnergy(predator.getMaxEnergy() / 2);
        long initialEnergy = predator.getCurrentEnergy();
        
        cell.addAnimal(predator);
        cell.addAnimal(victim);
        
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("wolf", true), 100);
        feedingService.tick(1);
        
        assertTrue(predator.getCurrentEnergy() > initialEnergy, "Wolf energy should increase after eating another wolf");
        assertEquals(1, cell.getAnimalCount());
    }

    @Test
    void testInterspeciesPredatorPredation() {
        // Wolf eats Fox
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("fox", true), 30);
        
        for (int i = 0; i < 5; i++) {
            GenericAnimal fox = new GenericAnimal(registry.getAnimalType(new SpeciesKey("fox", true)).orElseThrow());
            cell.addAnimal(fox);
        }
        
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() / 2);
        cell.addAnimal(wolf);
        
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("fox", true), 100);
        feedingService.tick(1);
        
        assertTrue(cell.getAnimalCount() < 6);
        assertTrue(wolf.isAlive());
    }

    @Test
    void testPreyHidingMechanic() {
        matrix.setChance(new SpeciesKey("wolf", true), new SpeciesKey("rabbit", false), 100);
        
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(new GenericAnimal(registry.getAnimalType(new SpeciesKey("rabbit", false)).orElseThrow()));
        }
        
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() / 2);
        cell.addAnimal(wolf);
        
        Animal target = (Animal) cell.getAnimals().get(0);
        target.setHiding(true);
        
        feedingService.tick(1);
        
        assertTrue(target.isAlive());
        assertTrue(cell.getAnimalCount() < 11);
    }
}