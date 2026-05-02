package com.island.nature.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.GenericAnimal;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import org.junit.jupiter.api.Test;

class ReproductionServiceTest {
    private final Configuration config = new Configuration();
    private final SpeciesRegistry registry = new SpeciesLoader(config).load();
    private final AnimalFactory factory = new AnimalFactory(registry, new DefaultRandomProvider());

    @Test
    void testReproductionWithMaxEnergy() {
        StatisticsService statisticsService = new StatisticsService(config);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        
        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .animalFactory(factory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .build();

        Island island = new Island(context, 1, 1);
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
        StatisticsService statisticsService = new StatisticsService(config);
        // Controlled random that always returns 0 for nextInt (meaning 0 offspring)
        RandomProvider zeroRandom = new RandomProvider() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public int nextInt(int origin, int bound) { return 0; }
            @Override public long nextLong() { return 0L; }
            @Override public double nextDouble() { return 0; }
            @Override public double nextDouble(double bound) { return 0; }
            @Override public boolean nextBoolean() { return false; }
        };

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .animalFactory(factory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(zeroRandom)
                .build();

        Island island = new Island(context, 1, 1);
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);

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
