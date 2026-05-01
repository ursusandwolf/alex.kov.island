package com.island.nature.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.GenericAnimal;
import com.island.nature.entities.SizeClass;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EntityContainerTest {
    private EntityContainer container;
    private SpeciesRegistry registry;

    @BeforeEach
    void setUp() {
        container = new EntityContainer();
        registry = new SpeciesLoader().load();
    }

    @Test
    @DisplayName("Should add and remove animals in O(1)")
    void testAddRemoveAnimal() {
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        Animal wolf = new GenericAnimal(wolfType);

        container.addAnimal(wolf);
        assertTrue(container.getAllAnimals().contains(wolf));
        assertEquals(1, container.countByType(wolfType));

        boolean removed = container.removeAnimal(wolf);
        assertTrue(removed);
        assertFalse(container.getAllAnimals().contains(wolf));
        assertEquals(0, container.countByType(wolfType));
    }

    @Test
    @DisplayName("Should index by predators and herbivores")
    void testRoleIndexing() {
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        AnimalType rabbitType = registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow();

        Animal wolf = new GenericAnimal(wolfType);
        Animal rabbit = new GenericAnimal(rabbitType);

        container.addAnimal(wolf);
        container.addAnimal(rabbit);

        assertTrue(container.getPredators().contains(wolf));
        assertFalse(container.getPredators().contains(rabbit));
        assertTrue(container.getHerbivores().contains(rabbit));
        assertFalse(container.getHerbivores().contains(wolf));
    }

    @Test
    @DisplayName("Should index by size class")
    void testSizeIndexing() {
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        SizeClass wolfSize = wolfType.getSizeClass();
        Animal wolf = new GenericAnimal(wolfType);

        container.addAnimal(wolf);
        Set<Animal> bySize = container.getBySize(wolfSize);
        assertTrue(bySize.contains(wolf));

        container.removeAnimal(wolf);
        assertFalse(container.getBySize(wolfSize).contains(wolf));
    }

    @Test
    @DisplayName("Should maintain deterministic order (LinkedHashSet)")
    void testDeterministicOrder() {
        AnimalType wolfType = registry.getAnimalType(SpeciesKey.WOLF).orElseThrow();
        Animal wolf1 = new GenericAnimal(wolfType);
        Animal wolf2 = new GenericAnimal(wolfType);
        Animal wolf3 = new GenericAnimal(wolfType);

        container.addAnimal(wolf1);
        container.addAnimal(wolf2);
        container.addAnimal(wolf3);

        Animal[] ordered = container.getAllAnimals().toArray(new Animal[0]);
        assertSame(wolf1, ordered[0]);
        assertSame(wolf2, ordered[1]);
        assertSame(wolf3, ordered[2]);
    }
}
