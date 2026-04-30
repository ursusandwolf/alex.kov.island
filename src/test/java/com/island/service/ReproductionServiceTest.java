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
    void testNoEnergyConsumedWhenNoOffspring() {
        Island island = new Island(1, 1, registry, new StatisticsService());
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);

        // Controlled random that always returns 0 for nextInt (meaning 0 offspring)
        com.island.util.RandomProvider zeroRandom = new com.island.util.RandomProvider() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public int nextInt(int origin, int bound) { return 0; }
            @Override public long nextLong() { return 0L; }
            @Override public double nextDouble() { return 0; }
            @Override public double nextDouble(double bound) { return 0; }
            @Override public boolean nextBoolean() { return false; }
        };

        Animal r1 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        Animal r2 = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());

        r1.setEnergy(r1.getMaxEnergy());
        r2.setEnergy(r2.getMaxEnergy());
        r1.checkAgeDeath(); // Age = 1
        r2.checkAgeDeath(); // Age = 1

        cell.addAnimal(r1);
        cell.addAnimal(r2);

        double energyBefore = r1.getCurrentEnergy();
        ReproductionService service = new ReproductionService(island, factory, registry, java.util.concurrent.Executors.newSingleThreadExecutor(), zeroRandom);
        service.tick(1);

        assertEquals(energyBefore, r1.getCurrentEnergy(), 0.000001, "Energy should not be consumed if 0 offspring were produced");
        assertEquals(2, cell.getAnimalCount());
    }
}
