package com.island.content;

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
    private final SpeciesConfig config = SpeciesConfig.getInstance();

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        matrix = new InteractionMatrix();
        feedingService = new FeedingService(island, matrix, Executors.newSingleThreadExecutor());
    }

    @Test
    @DisplayName("Test trophic hierarchy: predators hunt before prey acts")
    void testTrophicHierarchy() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
        Rabbit rabbit = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        
        wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        feedingService.run();
        
        assertFalse(rabbit.isAlive(), "Rabbit should be eaten by Wolf");
        assertEquals(1, cell.getAnimalCount(), "Only Wolf should remain");
    }

    @Test
    @DisplayName("Test escape protection: prey hides after failed attack")
    void testEscapeAndHide() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
        Fox fox = new Fox(config.getAnimalType(SpeciesKey.FOX));
        Rabbit rabbit = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        
        wolf.setEnergy(0.5);
        fox.setEnergy(0.5);

        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 1); 
        matrix.setChance(SpeciesKey.FOX, SpeciesKey.RABBIT, 100);
        
        cell.addAnimal(wolf);
        cell.addAnimal(fox);
        cell.addAnimal(rabbit);
        
        for (int i = 0; i < 100; i++) {
            feedingService.run();
            if (rabbit.isHiding() || !rabbit.isAlive()) break;
        }
        
        if (rabbit.isAlive()) {
            assertTrue(rabbit.isHiding(), "Rabbit should be hiding after wolf attack attempts");
            assertTrue(cell.getAnimals().contains(rabbit), "Rabbit should still be in cell because it hid from fox");
        }
    }
}
