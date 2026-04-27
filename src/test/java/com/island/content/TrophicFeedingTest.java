package com.island.content;

import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Fox;
import com.island.content.animals.predators.Wolf;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.service.FeedingService;
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
        
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.WOLF, 100);
        feedingService.run();
        
        assertTrue(predator.getCurrentEnergy() > initialEnergy, "Wolf energy should increase after eating another wolf");
        assertEquals(1, cell.getAnimalCount());
    }

    @Test
    void testInterspeciesPredatorPredation() {
        // Wolf eats Fox with 30% chance
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 30);
        
        // Add multiple foxes to avoid 'endangered' protection (threshold is 0.05 * 30 = 1.5)
        for (int i = 0; i < 5; i++) {
            Fox fox = new Fox(registry.getAnimalType(SpeciesKey.FOX).orElseThrow());
            cell.addAnimal(fox);
        }
        
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
        cell.addAnimal(wolf);
        
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.FOX, 100);
        feedingService.run();
        
        // One fox should be eaten, but we had 5. So at least one removal happened.
        assertTrue(cell.getAnimalCount() < 6);
        assertTrue(wolf.isAlive());
    }

    @Test
    void testPreyHidingMechanic() {
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        
        // Add many rabbits to avoid protection
        for (int i = 0; i < 10; i++) {
            cell.addAnimal(new Rabbit(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow()));
        }
        
        Wolf wolf = new Wolf(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
        cell.addAnimal(wolf);
        
        // Mark one as hiding manually
        Rabbit target = (Rabbit) cell.getHerbivores().get(0);
        target.setHiding(true);
        
        feedingService.run();
        
        // Wolf should have eaten another rabbit (not the hiding one)
        assertTrue(target.isAlive());
        assertTrue(cell.getAnimalCount() < 11);
    }
}
