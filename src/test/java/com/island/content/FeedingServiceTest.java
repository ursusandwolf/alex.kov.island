package com.island.content;

import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Wolf;
import com.island.engine.InteractionMatrix;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedingServiceTest {
    private Island island;
    private InteractionMatrix matrix;
    private FeedingService service;

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        matrix = new InteractionMatrix();
        // Setup 100% chance for wolf to eat rabbit
        matrix.setChance("wolf", "rabbit", 100);
        service = new FeedingService(island, matrix, java.util.concurrent.Executors.newSingleThreadExecutor());
    }

    @Test
    void testWolfEatsRabbit() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf();
        Rabbit rabbit = new Rabbit();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        wolf.consumeEnergy(2.0); // Make space for new energy
        double initialEnergy = wolf.getCurrentEnergy();
        
        service.run();
        
        // Rabbit should be gone
        assertEquals(1, cell.getAnimalCount());
        assertTrue(cell.getAnimals().contains(wolf));
        
        // Wolf should have more energy
        assertTrue(wolf.getCurrentEnergy() > initialEnergy);
    }
}
