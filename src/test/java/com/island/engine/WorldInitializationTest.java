package com.island.engine;

import com.island.content.SpeciesConfig;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WorldInitializationTest {

    @Test
    public void testWorldInitializationDensity() {
        Island island = new Island(16, 16);
        SpeciesConfig config = SpeciesConfig.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, config, executor);
        executor.shutdown();

        int totalOrganisms = island.getTotalOrganismCount();
        System.out.println("Total organisms after initialization: " + totalOrganisms);
        
        // With 16x16 = 256 cells, and each cell having ~10% of various species.
        // Even with just plants (max 200), 10% is 20 per cell.
        // 256 * 20 = 5120 plants alone on average.
        // Let's just verify it's not empty and has a reasonable amount.
        assertTrue(totalOrganisms > 1000, "Island should be significantly populated");
        
        // Check a random cell
        var cell = island.getCell(0, 0);
        assertTrue(cell.getAnimalCount() + cell.getPlantCount() > 0, "Cell 0,0 should not be empty");
    }
}
