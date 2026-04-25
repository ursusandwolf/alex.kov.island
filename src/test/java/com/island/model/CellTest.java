package com.island.model;

import com.island.content.animals.predators.Wolf;
import com.island.content.SpeciesConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {
    private Cell cell;
    private final SpeciesConfig config = SpeciesConfig.getInstance();

    @BeforeEach
    void setUp() {
        Island island = new Island(1, 1);
        cell = new Cell(0, 0, island);
    }

    @Test
    void testAddAnimal() {
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        boolean added = cell.addAnimal(wolf);
        assertTrue(added);
        assertEquals(1, cell.getAnimalCount());
        assertEquals(1, cell.countAnimalsBySpecies("wolf"));
    }

    @Test
    void testAddAnimalExceedingLimit() {
        int max = config.getAnimalType("wolf").getMaxPerCell();
        for (int i = 0; i < max; i++) {
            assertTrue(cell.addAnimal(new Wolf(config.getAnimalType("wolf"))), "Should be able to add wolf " + i);
        }
        assertFalse(cell.addAnimal(new Wolf(config.getAnimalType("wolf"))), "Should not be able to add wolf exceeding limit");
    }

    @Test
    void testRemoveAnimal() {
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        cell.addAnimal(wolf);
        boolean removed = cell.removeAnimal(wolf);
        assertTrue(removed);
        assertEquals(0, cell.getAnimalCount());
    }

    @Test
    void testCleanupDeadOrganisms() {
        Wolf aliveWolf = new Wolf(config.getAnimalType("wolf"));
        Wolf deadWolf = new Wolf(config.getAnimalType("wolf"));
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
