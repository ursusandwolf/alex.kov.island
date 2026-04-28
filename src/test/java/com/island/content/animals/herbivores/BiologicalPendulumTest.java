package com.island.content.animals.herbivores;

import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.content.plants.Grass;
import com.island.model.Cell;
import com.island.model.Island;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BiologicalPendulumTest {
    private final SpeciesRegistry registry = new SpeciesLoader().load();

    @Test
    void testCaterpillarPendulum() {
        Island island = new Island(1, 1);
        Cell cell = island.getCell(0, 0);
        
        Grass grass = new Grass(registry.getPlantWeight(SpeciesKey.PLANT) * registry.getPlantMaxCount(SpeciesKey.PLANT), 0);
        Caterpillar caterpillar = new Caterpillar(registry.getAnimalType(SpeciesKey.CATERPILLAR).orElseThrow().getWeight() 
                * registry.getAnimalType(SpeciesKey.CATERPILLAR).orElseThrow().getMaxPerCell(), 0);
        
        cell.addBiomass(grass);
        cell.addBiomass(caterpillar);
        
        double initialBiomass = caterpillar.getBiomass();
        double initialGrass = grass.getBiomass();
        
        caterpillar.processPendulum(cell);
        
        assertTrue(caterpillar.getBiomass() != initialBiomass, "Biomass should change after pendulum process");
    }
}
