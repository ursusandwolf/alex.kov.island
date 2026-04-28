package com.island.model;

import com.island.content.AnimalType;
import com.island.content.GenericAnimal;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CellTest {
    private Cell cell;
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @BeforeEach
    void setUp() {
        Island island = new Island(1, 1);
        cell = new Cell(0, 0, island);
    }

    @Test
    void testAddAnimal() {
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        GenericAnimal wolf = new GenericAnimal(wolfType);
        boolean added = cell.addAnimal(wolf);
        assertTrue(added);
        assertEquals(1, cell.getAnimalCount());
        assertEquals(1, cell.countAnimalsByType(wolfType));
    }

    @Test
    void testAddAnimalExceedingLimit() {
        int max = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow().getMaxPerCell();
        for (int i = 0; i < max; i++) {
            assertTrue(cell.addAnimal(new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow())), "Should be able to add wolf " + i);
        }
        assertFalse(cell.addAnimal(new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow())), "Should not be able to add wolf exceeding limit");
    }

    @Test
    void testRemoveAnimal() {
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        cell.addAnimal(wolf);
        boolean removed = cell.removeAnimal(wolf);
        assertTrue(removed);
        assertEquals(0, cell.getAnimalCount());
    }

    @Test
    void testCleanupDeadOrganisms() {
        GenericAnimal aliveWolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        GenericAnimal deadWolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
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
