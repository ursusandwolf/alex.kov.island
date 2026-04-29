package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.content.GenericBiomass;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Initializes the island world with organisms.
 */
public class WorldInitializer {

    public void initialize(Island island, SpeciesRegistry registry, AnimalFactory animalFactory, 
                           ExecutorService executor, com.island.util.RandomProvider random) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    initializeCell(cell, registry, animalFactory, random);
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("World initialization was interrupted: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesRegistry registry, AnimalFactory animalFactory, com.island.util.RandomProvider random) {
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            AnimalType type = registry.getAnimalType(species).orElse(null);
            if (type == null) {
                continue;
            }

            if (type.isBiomass() || type.isPlant()) {
                // Initialize biomass containers
                cell.addBiomass(new GenericBiomass(type));
                continue;
            }
            
            // Data-driven settlement for animals
            if (random.nextDouble() < type.getPresenceProb()) {
                double settlementRate = type.getSettlementBase() + (random.nextDouble() * type.getSettlementRange());
                int count = (int) (type.getMaxPerCell() * settlementRate);                
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    animalFactory.createInitialAnimal(species).ifPresent(cell::addAnimal);
                }
            }
        }
    }
}
