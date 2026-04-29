package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesKey;
import com.island.content.GenericAnimal;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.DefaultRandomProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReproductionServiceTest {
    private final SpeciesRegistry registry = new SpeciesLoader().load();
    private final AnimalFactory factory = new AnimalFactory(registry, new DefaultRandomProvider());

    @Test
    void testReproductionWithMaxEnergy() {
        Island island = new Island(1, 1, registry, new StatisticsService());
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        Animal r2 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        
        r1.setEnergy(r1.getMaxEnergy());
        r2.setEnergy(r2.getMaxEnergy());
        // Increment age to 1
        r1.checkAgeDeath();
        r2.checkAgeDeath();
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, registry, java.util.concurrent.Executors.newSingleThreadExecutor(), new DefaultRandomProvider());
        for (int i = 0; i < 20; i++) {
            service.tick(1);
        }
        
        // Expected: 2 parents + at least some babies.
        assertTrue(cell.getAnimalCount() >= 3, "Should produce at least 1 baby over 20 attempts");
    }

    @Test
    void testNoReproductionWhenStarving() {
        Island island = new Island(1, 1, registry, new StatisticsService());
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        Animal r2 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        
        r1.setEnergy(0.01);
        r2.setEnergy(0.01);
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, registry, java.util.concurrent.Executors.newSingleThreadExecutor(), new DefaultRandomProvider());
        service.tick(1);
        
        assertEquals(2, cell.getAnimalCount(), "Starving animals should not reproduce");
    }
}
