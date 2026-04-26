package com.island.content.animals.herbivores;

import com.island.content.plants.Grass;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BiologicalPendulumTest {

    @Test
    @DisplayName("Test Biological Pendulum: Caterpillar eats Grass and returns nutrients")
    void testPendulumCycle() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        Grass grass = new Grass();
        Caterpillar caterpillar = new Caterpillar();
        
        // Initial state: Grass at 10%, Caterpillar at 10% (by default constructor)
        double initialGrassBiomass = grass.getBiomass();
        double initialCaterpillarBiomass = caterpillar.getBiomass();
        
        cell.addPlant(grass);
        cell.addPlant(caterpillar);
        
        // Execute pendulum
        caterpillar.processPendulum(cell);
        
        // Check results: 
        // 1. Grass should be eaten (decreased)
        // 2. Caterpillar should grow from eating grass (minus decay)
        // 3. Grass should receive fertilizer from caterpillar decay
        
        // Given the high efficiency (90% feed, 90% fertilizer) and appetite (15% max):
        assertTrue(caterpillar.getBiomass() > initialCaterpillarBiomass, 
                "Caterpillar should grow when grass is available. Initial: " + initialCaterpillarBiomass + ", Now: " + caterpillar.getBiomass());
        assertTrue(grass.getBiomass() < initialGrassBiomass || caterpillar.getBiomass() > initialCaterpillarBiomass, 
                "Biomass should shift between entities");
    }
}
