package com.island.content;

import com.island.content.animals.herbivores.Rabbit;
import com.island.content.animals.predators.Wolf;
import com.island.util.InteractionMatrix;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedingServiceTest {
    private Island island;
    private InteractionMatrix matrix;
    private FeedingService service;
    private final SpeciesConfig config = SpeciesConfig.getInstance();

    @BeforeEach
    void setUp() {
        island = new Island(1, 1);
        island.setRedBookProtectionEnabled(false);
        matrix = new InteractionMatrix();
        matrix.setChance(SpeciesKey.WOLF, SpeciesKey.RABBIT, 100);
        service = new FeedingService(island, matrix, java.util.concurrent.Executors.newSingleThreadExecutor());
    }

    @Test
    void testWolfEatsRabbit() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf(config.getAnimalType(SpeciesKey.WOLF));
        Rabbit rabbit = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        wolf.consumeEnergy(2.0); 
        double initialEnergy = wolf.getCurrentEnergy();
        
        service.run();
        
        assertEquals(1, cell.getAnimalCount());
        assertTrue(cell.getAnimals().contains(wolf));
        assertTrue(wolf.getCurrentEnergy() > initialEnergy);
    }

    @Test
    void testRabbitEatsGrass() {
        Cell cell = island.getCell(0, 0);
        Rabbit rabbit = new Rabbit(config.getAnimalType(SpeciesKey.RABBIT));
        rabbit.setEnergy(0.05); 
        double initialEnergy = rabbit.getCurrentEnergy();

        com.island.content.plants.Grass grass = new com.island.content.plants.Grass();
        double initialBiomass = grass.getBiomass();
        cell.addPlant(grass);
        cell.addAnimal(rabbit);

        matrix.setChance(SpeciesKey.RABBIT, SpeciesKey.PLANT, 100);

        service.run();

        assertTrue(rabbit.getCurrentEnergy() > initialEnergy, "Rabbit should have gained energy from eating grass");
        assertTrue(grass.getBiomass() < initialBiomass, "Grass biomass should have decreased");
    }
}
