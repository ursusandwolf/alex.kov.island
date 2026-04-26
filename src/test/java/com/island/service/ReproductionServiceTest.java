package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesConfig;
import com.island.content.animals.herbivores.Rabbit;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReproductionServiceTest {
    private final SpeciesConfig config = SpeciesConfig.getInstance();
    private final AnimalFactory factory = new AnimalFactory(config);

    @Test
    void testReproductionWithMaxEnergy() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new Rabbit(config.getAnimalType("rabbit"));
        Animal r2 = new Rabbit(config.getAnimalType("rabbit"));
        
        // Give them max energy. saturation for rabbit is 0.45 kg.
        // Total energy = 0.45 * 2 = 0.90.
        // Max offspring for rabbit: base 2 + 1 bonus = 3.
        // Formula: E_per_head = 0.90 / (2 + 3) = 0.18.
        // Threshold: 0.4 * 0.45 = 0.18.
        // 0.18 >= 0.18 -> True. 
        // Expected: 2 parents + 3 babies = 5.
        r1.setEnergy(r1.getMaxEnergy());
        r2.setEnergy(r2.getMaxEnergy());
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        assertEquals(5, cell.getAnimalCount());
        
        for (Animal a : cell.getAnimals()) {
            assertEquals(0.18, a.getCurrentEnergy(), 0.001);
        }
    }

    @Test
    void testReproductionWithModerateEnergy() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new Rabbit(config.getAnimalType("rabbit"));
        Animal r2 = new Rabbit(config.getAnimalType("rabbit"));
        
        // Increase energy to be well above threshold.
        // saturation for rabbit is 0.45.
        // Threshold (40%) = 0.18.
        // Let's give them 0.35 each = 0.70 total.
        // 0.70 / 3 individuals = 0.233...
        // 0.233 >= 0.18 -> True.
        r1.setEnergy(0.35);
        r2.setEnergy(0.35);
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        assertEquals(3, cell.getAnimalCount());
    }

    @Test
    void testNoReproductionWhenStarving() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        Animal r1 = new Rabbit(config.getAnimalType("rabbit"));
        Animal r2 = new Rabbit(config.getAnimalType("rabbit"));
        
        // Total energy = 0.3.
        // For 1 baby need 0.54.
        r1.setEnergy(0.15);
        r2.setEnergy(0.15);
        
        cell.addAnimal(r1);
        cell.addAnimal(r2);
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        assertEquals(2, cell.getAnimalCount());
    }
}
