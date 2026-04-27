package com.island.content;

import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Fox;
import com.island.content.animals.predators.Wolf;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrophicFeedingTest {
    private Island island;
    private Cell cell;
    private InteractionMatrix matrix;
    private FeedingService feedingService;
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        cell = island.getCell(0, 0);
        matrix = new InteractionMatrix();
        feedingService = new FeedingService(island, matrix, registry, Executors.newSingleThreadExecutor());
    }

    @Test
    void testIntraspeciesPredation() {
        // Wolf eats Wolf (Intraspecies Predation) with 10% chance
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.WOLF, 10);
        
        Wolf predator = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        Wolf victim = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        
        predator.setEnergy(predator.getMaxEnergy() * 0.5);
        double initialEnergy = predator.getCurrentEnergy();
        
        cell.addAnimal(predator);
        cell.addAnimal(victim);
        
        // We might need multiple runs due to 10% chance, or just force successRate logic in a controlled provider.
        // For simplicity in this test, we run enough times or use 100% for verification.
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.WOLF, 100);
        feedingService.run();
        
        assertTrue(predator.getCurrentEnergy() > initialEnergy, "Wolf energy should increase after eating another wolf");
        assertEquals(1, cell.getAnimalCount());
    }

    @Test
    void testInterspeciesPredatorPredation() {
        // Wolf eats Fox with 30% chance
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 30);
        
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        Fox fox = new Fox(registry.getAnimalType(SpeciesKey.FOX).orElseThrow());
        
        wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
        
        cell.addAnimal(wolf);
        cell.addAnimal(fox);
        
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 100);
        feedingService.run();
        
        assertEquals(1, cell.getAnimalCount());
        assertTrue(wolf.isAlive());
    }

    @Test
    void testPreyHidingMechanic() {
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        Rabbit rabbit = new Rabbit(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        
        rabbit.setHiding(true); // Manually hide the rabbit
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        feedingService.run();
        
        assertEquals(2, cell.getAnimalCount(), "Hiding rabbit should not be eaten");
        assertTrue(rabbit.isAlive());
    }
}
