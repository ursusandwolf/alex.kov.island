package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.plants.Cabbage;
import com.island.content.plants.Grass;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Initializes the island world with organisms.
 */
public class WorldInitializer {

    public void initialize(Island island, SpeciesRegistry registry, AnimalFactory animalFactory, ExecutorService executor) {
        List<Callable<Void>> tasks = new ArrayList<>();
        
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    initializeCell(cell, registry, animalFactory);
                }
                return null;
            });
        }

        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Инициализация мира была прервана: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesRegistry registry, AnimalFactory animalFactory) {
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            AnimalType type = registry.getAnimalType(species).orElse(null);
            if (type == null) {
                continue;
            }

            double presenceProbability = type.isPredator() ? 0.4 : 0.8;
            
            if (species == SpeciesKey.BEAR) {
                presenceProbability = 0.15;
            }
            if (species == SpeciesKey.WOLF) {
                presenceProbability = 0.05;
            }
            
            if (ThreadLocalRandom.current().nextDouble() < presenceProbability) {
                double settlementRate = 0.10 + (ThreadLocalRandom.current().nextDouble() * 0.25);

                if (species == SpeciesKey.BEAR) {
                    settlementRate = 0.05 + (ThreadLocalRandom.current().nextDouble() * 0.05);
                }
                if (species == SpeciesKey.WOLF) {
                    settlementRate = 0.02 + (ThreadLocalRandom.current().nextDouble() * 0.03);
                }
                if (species == SpeciesKey.BUFFALO) {
                    settlementRate = 0.05;
                }

                int count = (int) (type.getMaxPerCell() * settlementRate);                
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    animalFactory.createAnimal(species).ifPresent(cell::addAnimal);
                }
            }
        }
        
        // Initialize plants with mass from registry
        cell.addPlant(new Grass(registry.getPlantWeight(SpeciesKey.PLANT) * registry.getPlantMaxCount(SpeciesKey.PLANT)));
        cell.addPlant(new Cabbage(registry.getPlantWeight(SpeciesKey.CABBAGE) * registry.getPlantMaxCount(SpeciesKey.CABBAGE)));
        
        // Caterpillar is also a biomass container
        registry.getAnimalType(SpeciesKey.CATERPILLAR).ifPresent(type -> {
            cell.addPlant(new Caterpillar(type.getWeight() * type.getMaxPerCell()));
        });
    }
}
