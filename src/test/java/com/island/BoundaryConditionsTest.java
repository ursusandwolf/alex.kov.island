package com.island;

import com.island.engine.SimulationBootstrap;
import com.island.engine.SimulationContext;
import com.island.model.Island;
import com.island.content.AnimalFactory;
import com.island.model.Cell;
import com.island.content.Animal;
import com.island.content.SpeciesKey;
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
                cell.getPlants().clear();
            }
        }
        
        // Run tick
        assertDoesNotThrow(() -> context.getGameLoop().runTick());
        
        int animalCount = 0;
        for (SpeciesKey key : SpeciesKey.values()) {
            if (key.isPredator() || (key != SpeciesKey.PLANT && key != SpeciesKey.GRASS && key != SpeciesKey.CABBAGE && key != SpeciesKey.CATERPILLAR)) {
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

        int maxWolves = context.getSpeciesConfig().getAnimalType(SpeciesKey.WOLF).getMaxPerCell();
        
        for (int i = 0; i < maxWolves + 10; i++) {
            factory.createAnimal(SpeciesKey.WOLF).ifPresent(cell::addAnimal);
        }
        
        int actualCount = cell.countAnimalsBySpecies(SpeciesKey.WOLF);
        
        assertTrue(actualCount <= maxWolves, "Cell should respect max capacity of wolves: " + actualCount + " vs " + maxWolves);
    }
}
