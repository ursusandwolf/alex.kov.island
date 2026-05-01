package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesLoader;
import com.island.content.WorldInitializer;
import com.island.model.Island;
import com.island.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorldInitializationTest {

    @Test
    public void testWorldInitializationDensity() {
        SpeciesRegistry registry = new SpeciesLoader().load();
        Island island = new Island(16, 16, registry, new StatisticsService());
        AnimalFactory animalFactory = new AnimalFactory(registry, new DefaultRandomProvider());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, registry, animalFactory, executor, new DefaultRandomProvider());
        executor.shutdown();

        int totalOrganisms = island.getTotalOrganismCount();
        System.out.println("Total organisms after initialization: " + totalOrganisms);
        
        assertTrue(totalOrganisms > 1000, "Island should be significantly populated");
        
        var cell = island.getCell(0, 0);
        assertTrue(cell.getAnimalCount() + cell.getPlantCount() > 0, "Cell 0,0 should not be empty");
    }
}
