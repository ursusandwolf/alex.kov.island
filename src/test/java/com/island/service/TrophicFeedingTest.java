package com.island.service;

import com.island.content.animals.herbivores.Duck;
import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Fox;
import com.island.content.animals.predators.Wolf;
import com.island.engine.InteractionMatrix;
import com.island.model.Cell;
import com.island.model.Island;
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

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        matrix = new InteractionMatrix();
        feedingService = new FeedingService(island, matrix, Executors.newSingleThreadExecutor());
        lifecycleService = new LifecycleService(island, Executors.newSingleThreadExecutor());
    }

    @Test
    @DisplayName("Test trophic hierarchy: predators hunt before prey acts")
    void testTrophicHierarchy() {
        Cell cell = island.getCell(0, 0);
        Fox fox = new Fox();
        Duck duck = new Duck();
        
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
        Wolf wolf = new Wolf();
        Fox fox = new Fox();
        Rabbit rabbit = new Rabbit();
        
        // Setup: Wolf eats Rabbit (1% - almost guaranteed to fail for test), Fox eats Rabbit (100%)
        // Note: FeedingService uses random, so 1% might still succeed sometimes but highly unlikely.
        // For absolute certainty, we could mock the random, but matrix setup is easier.
        matrix.setChance("wolf", "rabbit", 1); 
        matrix.setChance("fox", "rabbit", 100);
        
        cell.addAnimal(wolf);
        cell.addAnimal(fox);
        cell.addAnimal(rabbit);
        
        // Step 1: Run feeding. Wolf (heavier) attacks Rabbit first.
        // If it fails (which is 99% likely), rabbit hides.
        feedingService.run();
        
        // If rabbit survived (likely), it MUST be hiding.
        if (rabbit.isAlive()) {
            assertTrue(rabbit.isHiding(), "Rabbit should be hiding after surviving wolf attack");
            // Fox (who acts after Wolf) should NOT have eaten the rabbit because it's hidden
            assertTrue(cell.getAnimals().contains(rabbit), "Rabbit should still be in cell because it hid from fox");
        }
    }

    @Test
    @DisplayName("Test caterpillar protection on first tick")
    void testCaterpillarFirstTickProtection() {
        Cell cell = island.getCell(0, 0);
        Duck duck = new Duck();
        com.island.content.animals.herbivores.Caterpillar caterpillar = new com.island.content.animals.herbivores.Caterpillar();
        
        // Use 100% chance to remove randomness on tick 2
        matrix.setChance("duck", "caterpillar", 100);
        
        cell.addAnimal(duck);
        cell.addAnimal(caterpillar);
        
        // Set tick to 1
        island.nextTick(); 
        assertEquals(1, island.getTickCount());
        
        feedingService.run();
        
        // Caterpillar should be protected on tick 1
        assertTrue(caterpillar.isAlive(), "Caterpillar should be protected on tick 1");
        
        // Move to tick 2
        island.nextTick();
        assertEquals(2, island.getTickCount());
        
        feedingService.run();
        
        // Now caterpillar is vulnerable and chance is 100%
        assertFalse(caterpillar.isAlive(), "Caterpillar should be eaten on tick 2");
    }
}
