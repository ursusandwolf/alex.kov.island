package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.AnimalFactory;
import com.island.model.Cell;
import com.island.content.Animal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class BoundaryConditionsTest {

    @Test
    void testEmptyIsland() {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup();
        Island island = context.getIsland();
        
        // Remove everyone properly
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getGrid()[x][y];
                List<Animal> toRemove = new ArrayList<>(cell.getAnimals());
                for (Animal a : toRemove) {
                    cell.removeAnimal(a);
                }
                // Plants are not tracked by island counters in the same way, but let's clear them
                cell.getPlants().clear();
            }
        }
        
        // Run tick
        assertDoesNotThrow(() -> context.getGameLoop().runTick());
        // We only check animals here as plants have biomass and different counting
        int animalCount = 0;
        for (String key : context.getSpeciesConfig().getAllSpeciesKeys()) {
            if (!key.equals("plant") && !key.equals("cabbage") && !key.equals("caterpillar")) {
                animalCount += island.getSpeciesCount(key);
            }
        }
        assertEquals(0, animalCount, "Island should have no animals");
    }

    @Test
    void testOverpopulation() {
        SimulationBootstrap bootstrap = new SimulationBootstrap();
        SimulationContext context = bootstrap.setup();
        Island island = context.getIsland();
        AnimalFactory factory = new AnimalFactory(context.getSpeciesConfig());
        
        Cell cell = island.getGrid()[0][0];
        // Clear cell first to have exact count
        List<Animal> toRemove = new ArrayList<>(cell.getAnimals());
        for (Animal a : toRemove) cell.removeAnimal(a);

        int maxWolves = context.getSpeciesConfig().getAnimalType("wolf").getMaxPerCell();
        
        for (int i = 0; i < maxWolves + 10; i++) {
            cell.addAnimal(factory.createAnimal("wolf"));
        }
        
        int actualCount = 0;
        for (Animal a : cell.getAnimals()) {
            if (a.getSpeciesKey().equals("wolf")) actualCount++;
        }
        
        assertTrue(actualCount <= maxWolves, "Cell should respect max capacity of wolves: " + actualCount + " vs " + maxWolves);
    }
}
