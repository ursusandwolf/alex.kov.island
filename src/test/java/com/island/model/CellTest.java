package com.island.model;

import com.island.content.AnimalType;
import com.island.content.animals.predators.Wolf;
import com.island.content.SpeciesConfig;
import com.island.content.SpeciesKey;
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
        AnimalType wolfType = config.getAnimalType(SpeciesKey.WOLF);
        Wolf wolf = new Wolf(wolfType);
        boolean added = cell.addAnimal(wolf);
        assertTrue(added);
        assertEquals(1, cell.getAnimalCount());
        assertEquals(1, cell.countAnimalsByType(wolfType));
    }

    @Test
    void testAddAnimalExceedingLimit() {
        int max = config.getAnimalType(SpeciesKey.WOLF).getMaxPerCell();
        for (int i = 0; i < max; i++) {
            assertTrue(cell.addAnimal(new Wolf(config.getAnimalType(SpeciesKey.WOLF))), "Should be able to add wolf " + i);
        }
        assertFalse(cell.addAnimal(new Wolf(config.getAnimalType(SpeciesKey.WOLF))), "Should not be able to add wolf exceeding limit");
    }

    @Test
    void testRemoveAnimal() {
        Wolf wolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
        cell.addAnimal(wolf);
        boolean removed = cell.removeAnimal(wolf);
        assertTrue(removed);
        assertEquals(0, cell.getAnimalCount());
    }

    @Test
    void testCleanupDeadOrganisms() {
        Wolf aliveWolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
        Wolf deadWolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
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
