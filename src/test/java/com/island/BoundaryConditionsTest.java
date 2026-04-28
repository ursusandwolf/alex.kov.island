package com.island;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesKey;
import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BoundaryConditionsTest {
    private SimulationContext context;
    private AnimalFactory factory;
    private Cell cell;

    @BeforeEach
    void setUp() {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        context = bootstrap.setup();
        factory = new AnimalFactory(context.getSpeciesRegistry());
        cell = context.getIsland().getCell(0, 0);
    }

    @Test
    void testToroidalBoundaries() {
        Island island = context.getIsland();
        int w = island.getWidth();
        int h = island.getHeight();

        // Testing getCell with out of bounds coordinates
        Cell c1 = island.getCell(w, h);      // Should be (0,0)
        Cell c2 = island.getCell(-1, -1);    // Should be (w-1, h-1)
        
        assertTrue(c1 == island.getCell(0, 0));
        assertTrue(c2 == island.getCell(w - 1, h - 1));
    }

    @Test
    void testMaxAnimalCapacityInCell() {
        // Clear cell first to have exact count
        List<Animal> toRemove = cell.getAnimals();
        for (Animal a : toRemove) cell.removeAnimal(a);

        int maxWolves = context.getSpeciesRegistry().getAnimalType(SpeciesKey.WOLF).orElseThrow().getMaxPerCell();

        for (int i = 0; i < maxWolves + 10; i++) {
            factory.createAnimal(SpeciesKey.WOLF).ifPresent(cell::addAnimal);
        }

        int actualCount = cell.countAnimalsByType(context.getSpeciesRegistry().getAnimalType(SpeciesKey.WOLF).orElseThrow());

        assertTrue(actualCount <= maxWolves, "Cell should respect max capacity of wolves: " + actualCount + " vs " + maxWolves);
    }
}
