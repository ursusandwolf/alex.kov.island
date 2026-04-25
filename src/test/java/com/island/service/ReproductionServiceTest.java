package com.island.service;

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
    void testReproduction() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        cell.addAnimal(new Rabbit(config.getAnimalType("rabbit")));
        cell.addAnimal(new Rabbit(config.getAnimalType("rabbit")));
        
        assertEquals(2, cell.getAnimalCount());
        
        ReproductionService service = new ReproductionService(island, factory, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        // 2 rabbits -> 1 pair -> 3 babies. Total 5.
        assertEquals(5, cell.getAnimalCount());
    }
}
