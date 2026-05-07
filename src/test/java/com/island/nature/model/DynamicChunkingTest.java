package com.island.nature.model;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.domain.NatureDomainContextFactory;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.common.RandomProvider;
import com.island.nature.entities.core.Organism;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Collection;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DynamicChunkingTest {
    private Configuration config;
    private Island island;
    private AnimalFactory animalFactory;

    @BeforeEach
    void setUp() {
        config = new Configuration();
        config.setIslandWidth(10);
        config.setIslandHeight(10);
        config.setDynamicChunkingEnabled(true);
        config.setDynamicChunkingTargetLoad(10);
        config.setDynamicChunkingMinSize(1);
        
        NatureDomainContext context = NatureDomainContextFactory.create(config);
        island = new Island(context, 10, 10, new DefaultEventBus());
        animalFactory = context.getAnimalFactory();
    }

    @Test
    void shouldCreateMoreChunksWhenDensityIsHigh() {
        // Initially world is empty, should have few chunks
        int initialChunks = island.getChunks().size();
        
        // Add many animals to one corner (0,0)
        SpeciesKey wolfKey = new SpeciesKey("wolf", true);
        Cell targetCell = island.getGrid()[0][0];
        for (int i = 0; i < 50; i++) {
            animalFactory.createInitialAnimal(wolfKey).ifPresent(targetCell::addAnimal);
        }

        // Rebalance
        island.rebalance();
        
        int newChunks = island.getChunks().size();
        assertTrue(newChunks > initialChunks, "Should have more chunks after adding entities: " + newChunks + " vs " + initialChunks);
        
        // Verify that chunks covering (0,0) are smaller
        List<Chunk> chunks = (List<Chunk>) island.getChunks();
        Chunk cornerChunk = chunks.stream()
                .filter(c -> c.getCells().stream().anyMatch(cell -> cell.getX() == 0 && cell.getY() == 0))
                .findFirst().orElseThrow();
        
        assertTrue(cornerChunk.getCells().size() < 100, "Corner chunk should be smaller than whole grid");
    }

    @Test
    void shouldReduceChunksWhenWorldIsSparse() {
        // High density initially
        config.setDynamicChunkingTargetLoad(5);
        
        SpeciesKey wolfKey = new SpeciesKey("wolf", true);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                for (int i = 0; i < 10; i++) {
                    animalFactory.createInitialAnimal(wolfKey).ifPresent(island.getGrid()[x][y]::addAnimal);
                }
            }
        }
        
        island.rebalance();
        int highDensityChunks = island.getChunks().size();
        
        // Clear all animals
        for (int x = 0; x < 10; x++) {
            for (int y = 0; y < 10; y++) {
                Cell cell = island.getGrid()[x][y];
                List<Organism> entities = List.copyOf(cell.getEntities());
                entities.forEach(e -> {
                    if (e instanceof Animal a) cell.removeAnimal(a);
                });
            }
        }
        
        island.rebalance();
        int lowDensityChunks = island.getChunks().size();
        
        assertTrue(lowDensityChunks < highDensityChunks, "Should have fewer chunks when sparse: " + lowDensityChunks + " vs " + highDensityChunks);
    }
}
