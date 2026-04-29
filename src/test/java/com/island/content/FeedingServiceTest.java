package com.island.content;

import com.island.content.GenericAnimal;
import com.island.content.plants.Grass;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.service.FeedingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class FeedingServiceTest {
    private Island island;
    private InteractionMatrix matrix;
    private FeedingService service;
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        matrix = new InteractionMatrix();
        // Wolf eats Rabbit with 100% chance
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        // Rabbit eats Grass with 100% chance
        matrix.setChance(SpeciesKey.RABBIT, SpeciesKey.PLANT, 100);
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        service = new FeedingService(island, matrix, registry, huntingStrategy, Executors.newSingleThreadExecutor(), new com.island.util.DefaultRandomProvider());
    }

    @Test
    void testWolfEatsRabbit() {
        Cell cell = island.getCell(0, 0);
        GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
        GenericAnimal rabbit = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        
        // Ensure wolf is hungry but has enough energy to hunt
        wolf.setEnergy(wolf.getMaxEnergy() * 0.5);
        double initialEnergy = wolf.getCurrentEnergy();
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        service.run();
        
        assertTrue(wolf.getCurrentEnergy() > initialEnergy, "Wolf energy should increase after eating rabbit");
        assertEquals(0, cell.getHerbivores().size(), "Rabbit should be eaten and removed from cell");
        assertFalse(rabbit.isAlive(), "Rabbit should be dead");
    }

    @Test
    void testRabbitEatsGrass() {
        Cell cell = island.getCell(0, 0);
        GenericAnimal rabbit = new GenericAnimal(registry.getAnimalType(SpeciesKey.RABBIT).orElseThrow());
        rabbit.setEnergy(rabbit.getMaxEnergy() * 0.5);
        double initialEnergy = rabbit.getCurrentEnergy();
        
        Grass grass = new Grass(registry.getPlantWeight(SpeciesKey.PLANT) * registry.getPlantMaxCount(SpeciesKey.PLANT), 0);
        cell.addAnimal(rabbit);
        cell.addBiomass(grass);
        
        service.run();
        
        assertTrue(rabbit.getCurrentEnergy() > initialEnergy, "Rabbit energy should increase after eating grass");
        assertTrue(grass.getBiomass() < registry.getPlantWeight(SpeciesKey.PLANT) * registry.getPlantMaxCount(SpeciesKey.PLANT), "Grass biomass should decrease");
    }
}
