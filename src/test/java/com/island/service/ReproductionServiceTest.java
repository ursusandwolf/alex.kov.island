package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.content.SpeciesKey;
import com.island.content.animals.herbivores.Rabbit;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproductionServiceTest {
    private final SpeciesConfig config = SpeciesConfig.getInstance();
    private final AnimalFactory factory = new AnimalFactory(config);

    @Test
    void testReproductionWithMaxEnergy() {
        Island island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        Animal r2 = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        
        r1.setEnergy(r1.getMaxEnergy());
        r2.setEnergy(r2.getMaxEnergy());
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        // Expected: 2 parents + (1 to 3) babies.
        assertTrue(cell.getAnimalCount() >= 3 && cell.getAnimalCount() <= 5, "Should produce at least 1 baby");
    }

    @Test
    void testNoReproductionWhenStarving() {
        Island island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        Animal r2 = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        
        r1.setEnergy(0.01);
        r2.setEnergy(0.01);
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        assertEquals(2, cell.getAnimalCount(), "Starving animals should not reproduce");
    }
}
