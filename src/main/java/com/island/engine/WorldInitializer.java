package com.island.engine;

import static com.island.config.SimulationConstants.DEFAULT_HERBIVORE_PRESENCE_PROB;
import static com.island.config.SimulationConstants.DEFAULT_PREDATOR_PRESENCE_PROB;

import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.content.animals.herbivores.Butterfly;
import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.plants.Cabbage;
import com.island.content.plants.Grass;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.util.RandomUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

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
            System.err.println("World initialization was interrupted: " + e.getMessage());
        }
    }

    private void initializeCell(Cell cell, SpeciesRegistry registry, AnimalFactory animalFactory) {
        for (SpeciesKey species : animalFactory.getRegisteredSpecies()) {
            AnimalType type = registry.getAnimalType(species).orElse(null);
            if (type == null) {
                continue;
            }

            // 1. Presence Probability (Filter which species exist in this cell)
            double presenceProbability = type.isPredator() ? DEFAULT_PREDATOR_PRESENCE_PROB : DEFAULT_HERBIVORE_PRESENCE_PROB;
            
            // Special cases for rare predators
            if (species == SpeciesKey.BEAR) {
                presenceProbability = 0.15;
            }
            if (species == SpeciesKey.WOLF) {
                presenceProbability = 0.10;
            }
            
            if (RandomUtils.nextDouble() < presenceProbability) {
                // 2. Settlement Rate (Density of the species if present)
                // Implement a Population Pyramid: smaller = denser
                double baseRate;
                double randomRange;

                switch (type.getSizeClass()) {
                    case TINY -> { // Mouse, Hamster, Caterpillar
                        baseRate = 0.30;
                        randomRange = 0.20;
                    }
                    case SMALL -> { // Duck
                        baseRate = 0.20;
                        randomRange = 0.15;
                    }
                    case NORMAL -> { // Rabbit, Fox, Eagle
                        baseRate = 0.10;
                        randomRange = 0.10;
                    }
                    case MEDIUM -> { // Wolf, Boa, Goat, Sheep
                        baseRate = 0.05;
                        randomRange = 0.05;
                    }
                    default -> { // Large, Huge (Bear, Buffalo, Horse, Deer)
                        baseRate = 0.02;
                        randomRange = 0.03;
                    }
                }

                double settlementRate = baseRate + (RandomUtils.nextDouble() * randomRange);
                int count = (int) (type.getMaxPerCell() * settlementRate);                
                count = Math.max(count, 1);
                
                for (int i = 0; i < count; i++) {
                    animalFactory.createInitialAnimal(species).ifPresent(cell::addAnimal);
                }
            }
        }
        
        // Initialize plants with mass from registry
        cell.addBiomass(new Grass(registry.getPlantWeight(SpeciesKey.GRASS) * registry.getPlantMaxCount(SpeciesKey.GRASS), registry.getPlantSpeed(SpeciesKey.GRASS)));
        cell.addBiomass(new Cabbage(registry.getPlantWeight(SpeciesKey.CABBAGE) * registry.getPlantMaxCount(SpeciesKey.CABBAGE), registry.getPlantSpeed(SpeciesKey.CABBAGE)));
        
        // Caterpillar and Butterfly are also biomass containers
        registry.getAnimalType(SpeciesKey.CATERPILLAR).ifPresent(type -> {
            cell.addBiomass(new Caterpillar(type.getWeight() * type.getMaxPerCell(), type.getSpeed()));
        });
        registry.getAnimalType(SpeciesKey.BUTTERFLY).ifPresent(type -> {
            cell.addBiomass(new Butterfly(type.getWeight() * type.getMaxPerCell() * 0.1, type.getSpeed()));
        });
    }
}
