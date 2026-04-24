package com.island.service;

import com.island.content.animals.herbivores.Rabbit;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReproductionServiceTest {

    @Test
    void testReproduction() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        cell.addAnimal(new Rabbit());
        cell.addAnimal(new Rabbit());
        
        assertEquals(2, cell.getAnimalCount());
        
        ReproductionService service = new ReproductionService(island, java.util.concurrent.Executors.newSingleThreadExecutor());
        service.run();
        
        // Should have 3 rabbits now (2 parents + 1 baby)
        assertEquals(3, cell.getAnimalCount());
    }
}
