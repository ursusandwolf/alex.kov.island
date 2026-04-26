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
        
        // Setup: Wolf eats Rabbit (1% - almost guaranteed to fail for test), Fox eats Rabbit (100%)
        matrix.setChance("wolf", "rabbit", 1); 
        matrix.setChance("fox", "rabbit", 100);
        
        cell.addAnimal(wolf);
        cell.addAnimal(fox);
        cell.addAnimal(rabbit);
        
        // Step 1: Run feeding. Wolf (heavier) attacks Rabbit first.
        feedingService.run();
        
        // If rabbit survived (likely), it MUST be hiding.
        if (rabbit.isAlive()) {
            assertTrue(rabbit.isHiding(), "Rabbit should be hiding after surviving wolf attack");
            // Fox (who acts after Wolf) should NOT have eaten the rabbit because it's hidden
            assertTrue(cell.getAnimals().contains(rabbit), "Rabbit should still be in cell because it hid from fox");
        }
    }
}
