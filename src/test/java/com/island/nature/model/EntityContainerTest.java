package com.island.nature.model;

import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.config.Configuration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.GenericAnimal;
import com.island.nature.entities.core.SizeClass;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;

class EntityContainerTest {
    private EntityContainer container;
    private SpeciesRegistry registry;
    private ComponentRegistry componentRegistry;

    @BeforeEach
    void setUp() {
        Configuration config = new Configuration();
        container = new EntityContainer(config);
        registry = new SpeciesLoader(config).load();
        componentRegistry = new ComponentRegistry();
    }

    @Test
    @DisplayName("Should add and remove animals")
    void testAddRemoveAnimal() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        Animal wolf = new GenericAnimal(wolfType, componentRegistry);

        container.addAnimal(wolf);
        assertTrue(container.getAllAnimals().contains(wolf));
        assertEquals(1, container.countByType(wolfType));

        boolean removed = container.removeAnimal(wolf);
        assertTrue(removed);
        assertFalse(container.getAllAnimals().contains(wolf));
        assertEquals(0, container.countByType(wolfType));
    }

    @Test
    @DisplayName("Should maintain order in lists")
    void testOrder() {
        AnimalType wolfType = registry.getAnimalType(new SpeciesKey("wolf", true)).orElseThrow();
        Animal wolf1 = new GenericAnimal(wolfType, componentRegistry);
        Animal wolf2 = new GenericAnimal(wolfType, componentRegistry);
        Animal wolf3 = new GenericAnimal(wolfType, componentRegistry);

        container.addAnimal(wolf1);
        container.addAnimal(wolf2);
        container.addAnimal(wolf3);

        java.util.List<Animal> ordered = container.getByType(wolfType);
        assertSame(wolf1, ordered.get(0));
        assertSame(wolf2, ordered.get(1));
        assertSame(wolf3, ordered.get(2));
    }
}