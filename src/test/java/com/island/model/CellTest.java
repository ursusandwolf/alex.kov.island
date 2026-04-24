package com.island.model;

import com.island.content.animals.predators.Wolf;
import com.island.content.animals.herbivores.Rabbit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {
    private Cell cell;

    @BeforeEach
    void setUp() {
        cell = new Cell(0, 0);
    }

    @Test
    void testAddAnimal() {
        Wolf wolf = new Wolf();
        boolean added = cell.addAnimal(wolf);
        assertTrue(added);
        assertEquals(1, cell.getAnimalCount());
        assertEquals(1, cell.countAnimalsBySpecies("wolf"));
    }

    @Test
    void testAddAnimalExceedingLimit() {
        // Limit for wolf is usually 30 (checking species.properties)
        // We will add more than maxPerCell to verify limit
        int max = new Wolf().getMaxPerCell();
        for (int i = 0; i < max; i++) {
            assertTrue(cell.addAnimal(new Wolf()), "Should be able to add wolf " + i);
        }
        assertFalse(cell.addAnimal(new Wolf()), "Should not be able to add wolf exceeding limit");
    }

    @Test
    void testRemoveAnimal() {
        Wolf wolf = new Wolf();
        cell.addAnimal(wolf);
        boolean removed = cell.removeAnimal(wolf);
        assertTrue(removed);
        assertEquals(0, cell.getAnimalCount());
    }

    @Test
    void testCleanupDeadOrganisms() {
        Wolf aliveWolf = new Wolf();
        Wolf deadWolf = new Wolf();
        deadWolf.consumeEnergy(deadWolf.getMaxEnergy()); // Kill it
        
        cell.addAnimal(aliveWolf);
        cell.addAnimal(deadWolf);
        
        assertEquals(2, cell.getAnimalCount());
        cell.cleanupDeadOrganisms();
        assertEquals(1, cell.getAnimalCount());
        assertTrue(cell.getAnimals().contains(aliveWolf));
        assertFalse(cell.getAnimals().contains(deadWolf));
    }
}
