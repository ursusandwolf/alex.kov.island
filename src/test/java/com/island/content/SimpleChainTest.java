package com.island.content;

import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.util.RandomUtils;
import com.island.util.RandomProvider;
import com.island.util.DefaultRandomProvider;
import com.island.service.FeedingService;
import com.island.content.plants.Grass;
import com.island.content.animals.predators.Bear;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.Executors;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class SimpleChainTest {
    private Island island;
    private Cell cell;
    private FeedingService feedingService;
    private SpeciesRegistry registry;
    private InteractionMatrix matrix;

    @BeforeEach
    void setUp() {
        registry = new SpeciesLoader().load();
        island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false); // Disable Red Book protection for deterministic testing
        cell = island.getCell(0, 0);
        matrix = InteractionMatrix.buildFrom(registry);
        
        // Force 100% success for deterministic testing
        matrix.setChance(SpeciesKey.FOX, SpeciesKey.DUCK, 100);
        matrix.setChance(SpeciesKey.DUCK, SpeciesKey.PLANT, 100);
        
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        feedingService = new FeedingService(island, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), new DefaultRandomProvider());
    }

    @Test
    void testFoxMissesDuckHidesSecondFoxFails() {
        // 1. Setup: Two Foxes and one Duck
        GenericAnimal fox1 = new GenericAnimal(registry.getAnimalType(SpeciesKey.FOX).orElseThrow());
        GenericAnimal fox2 = new GenericAnimal(registry.getAnimalType(SpeciesKey.FOX).orElseThrow());
        GenericAnimal duck = new GenericAnimal(registry.getAnimalType(SpeciesKey.DUCK).orElseThrow());
        
        fox1.setEnergy(fox1.getMaxEnergy());
        fox2.setEnergy(fox2.getMaxEnergy());
        
        cell.addAnimal(duck);
        cell.addAnimal(fox1);
        cell.addAnimal(fox2);

        // Force 100% success chance in matrix, but we will control the actual outcome via RandomProvider
        matrix.setChance(SpeciesKey.FOX, SpeciesKey.DUCK, 100);

        // Custom RandomProvider to simulate:
        // 1. First hunt attempt: fail (1.0 >= 1.0 successRate)
        // 2. Any subsequent attempt: would be success (0.0), but duck should be hidden
        RandomProvider customRandom = new RandomProvider() {
            private int callCount = 0;
            @Override public int nextInt(int bound) { return 0; }
            @Override public int nextInt(int origin, int bound) { return origin; }
            @Override public double nextDouble() { 
                return (callCount++ == 0) ? 1.0 : 0.0; // Fail first call, succeed others
            }
            @Override public double nextDouble(double bound) { return 0; }
            @Override public boolean checkChance(int chance) { return false; }
        };
        
        // Re-create service with custom random for this specific test
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        feedingService = new FeedingService(island, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), customRandom);

        // 2. Execute Feeding
        feedingService.tick(1);

            // 3. Assertions
            assertTrue(duck.isAlive(), "Duck should be alive because fox1 missed and fox2 couldn't find her");
            assertTrue(duck.isHiding(), "Duck should be hiding after fox1 missed");
            
            // Energy check: fox1 should have lost energy for the hunt effort
            assertTrue(fox1.getCurrentEnergy() < fox1.getMaxEnergy(), "Fox1 should have spent energy on the hunt attempt");
            
            // Fox2 should have NOT even attempted to hunt because duck is hidden
            // In our system, if no prey is found in buffet, no energy is spent on 'hunt attempt'
            assertEquals(fox2.getMaxEnergy(), fox2.getCurrentEnergy(), 0.001, "Fox2 should not have spent energy because Duck was hidden");
    }

    @Test
    void testWolfPackHuntsBear() {
        // 1. Setup: 10 Wolves and 1 Bear
        List<GenericAnimal> wolves = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
            wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
            wolves.add(wolf);
            cell.addAnimal(wolf);
        }

        Bear bear = new Bear(registry.getAnimalType(SpeciesKey.BEAR).orElseThrow());
        // Age the bear so it's not hibernating (hibernates if age % 150 < 50)
        for (int i = 0; i < 60; i++) {
            bear.checkAgeDeath();
        }
        assertFalse(bear.isHibernating(), "Bear should be active at age 60");
        cell.addAnimal(bear);

        // Ensure 100% success for deterministic testing
        RandomProvider customRandom = new RandomProvider() {
            @Override public int nextInt(int bound) { return 0; }
            @Override public int nextInt(int origin, int bound) { return origin; }
            @Override public double nextDouble() { return 0.0; } // 100% success
            @Override public double nextDouble(double bound) { return 0; }
            @Override public boolean checkChance(int chance) { return true; }
        };
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        feedingService = new FeedingService(island, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), customRandom);

        feedingService.tick(1);

        assertFalse(bear.isAlive(), "Bear should be eaten by wolf pack");
        for (GenericAnimal wolf : wolves) {
            assertTrue(wolf.getCurrentEnergy() > wolf.getMaxEnergy() * 0.5, "Wolves should gain energy");
        }
    }

    @Test
    void testSleepingBearIsInvisible() {
        // 1. Setup: 10 Wolves and 1 Sleeping Bear
        for (int i = 0; i < 10; i++) {
            GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
            cell.addAnimal(wolf);
        }

        // Bear hibernates when age % 150 < 50. Age 0 is sleeping.
        Bear bear = new Bear(registry.getAnimalType(SpeciesKey.BEAR).orElseThrow());
        assertTrue(bear.isHibernating(), "Bear should be hibernating at age 0");
        cell.addAnimal(bear);

        feedingService.tick(1);

        assertTrue(bear.isAlive(), "Sleeping bear should be invisible to wolf pack");
    }
}
