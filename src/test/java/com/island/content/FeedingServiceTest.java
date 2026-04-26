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
        // Setup 100% chance for wolf to eat rabbit
        matrix.setChance("wolf", "rabbit", 100);
        service = new FeedingService(island, matrix, java.util.concurrent.Executors.newSingleThreadExecutor());
    }

    @Test
    void testWolfEatsRabbit() {
        Cell cell = island.getCell(0, 0);
        Wolf wolf = new Wolf(config.getAnimalType("wolf"));
        Rabbit rabbit = new Rabbit(config.getAnimalType("rabbit"));
        
        cell.addAnimal(wolf);
        cell.addAnimal(rabbit);
        
        wolf.consumeEnergy(2.0); // Make space for new energy
        double initialEnergy = wolf.getCurrentEnergy();
        
        service.run();
        
        // Rabbit should be gone
        assertEquals(1, cell.getAnimalCount());
        assertTrue(cell.getAnimals().contains(wolf));
        
        // Wolf should have more energy
        assertTrue(wolf.getCurrentEnergy() > initialEnergy);
    }

    @Test
    void testRabbitEatsGrass() {
        Cell cell = island.getCell(0, 0);
        Rabbit rabbit = new Rabbit(config.getAnimalType("rabbit"));
        // saturation for rabbit is 0.45kg. Give it very low energy but keep it alive.
        rabbit.setEnergy(0.05); 
        double initialEnergy = rabbit.getCurrentEnergy();

        com.island.content.plants.Grass grass = new com.island.content.plants.Grass();
        double initialBiomass = grass.getBiomass();
        cell.addPlant(grass);
        cell.addAnimal(rabbit);

        // Setup matrix: Rabbit eats Plant (Grass) with 100% chance
        matrix.setChance("rabbit", "Plant", 100);

        service.run();

        // Rabbit should have gained energy
        assertTrue(rabbit.getCurrentEnergy() > initialEnergy, "Rabbit should have gained energy from eating grass");
        assertTrue(grass.getBiomass() < initialBiomass, "Grass biomass should have decreased");
    }
}
