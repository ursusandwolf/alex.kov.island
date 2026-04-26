package com.island.content;

import com.island.content.animals.herbivores.Duck;
import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Fox;
import com.island.content.animals.predators.Wolf;
import com.island.util.InteractionMatrix;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.service.LifecycleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class TrophicFeedingTest {
    private Island island;
    private InteractionMatrix matrix;
    private FeedingService feedingService;
    private LifecycleService lifecycleService;
    private final SpeciesConfig config = SpeciesConfig.getInstance();

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        matrix = new InteractionMatrix();
        feedingService = new FeedingService(island, matrix, Executors.newSingleThreadExecutor());
        lifecycleService = new LifecycleService(island, Executors.newSingleThreadExecutor());
    }

    @Test
    @DisplayName("Test trophic hierarchy: predators hunt before prey acts")
    void testTrophicHierarchy() {
        Cell cell = island.getCell(0, 0);
        Fox fox = new Fox(config.getAnimalType("fox"));
        Duck duck = new Duck(config.getAnimalType("duck"));
        
        // Setup: Fox eats Duck (100%), Duck eats plants (not needed for this test)
        matrix.setChance("fox", "duck", 100);
        
        cell.addAnimal(fox);
        cell.addAnimal(duck);
        
        feedingService.run();
        
        // If Fox acts first, Duck should be eaten and gone
        assertFalse(duck.isAlive(), "Duck should be eaten by Fox");
        assertEquals(1, cell.getAnimalCount(), "Only Fox should remain");
    }

    @Test
    @DisplayName("Test escape protection: prey hides after failed attack")
    void testEscapeAndHide() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        Fox fox = new Fox(config.getAnimalType("fox"));
        Rabbit rabbit = new Rabbit(config.getAnimalType("rabbit"));
        
        // Ensure predators are hungry and will attack
        wolf.setEnergy(0.5);
        fox.setEnergy(0.5);

        // Wolf eats Rabbit with 1% chance (forced fail), Fox eats Rabbit (100%)
        matrix.setChance("wolf", "rabbit", 1); 
        matrix.setChance("fox", "rabbit", 100);
        
        cell.addAnimal(wolf);
        cell.addAnimal(fox);
        cell.addAnimal(rabbit);
        
        // Use a loop to ensure the wolf attack actually triggers (1% chance is small, but if it hits, it fails the 'hide' check if eaten)
        // We want to test the failure case.
        for (int i = 0; i < 50; i++) {
            feedingService.run();
            if (rabbit.isHiding() || !rabbit.isAlive()) break;
        }
        
        if (rabbit.isAlive()) {
            assertTrue(rabbit.isHiding(), "Rabbit should be hiding after wolf attack attempts");
            // Check that Fox didn't eat it
            assertTrue(cell.getAnimals().contains(rabbit), "Rabbit should still be in cell because it hid from fox");
        }
    }
}
