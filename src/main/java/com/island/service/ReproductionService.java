package com.island.service;

import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.Plant;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

public class ReproductionService implements Runnable {
    private final Island island;
    private final ExecutorService executor;

    public ReproductionService(Island island, ExecutorService executor) {
        this.island = island;
        this.executor = executor;
    }

    @Override
    public void run() {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    processCell(cell);
                }
                return null;
            });
        }
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processCell(Cell cell) {
        // 1. Reproduction of Animals (requires pairs)
        reproduceAnimals(cell);
        
        // 2. Reproduction of Plants (polymorphic behavior)
        reproducePlants(cell);
    }

    private void reproduceAnimals(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        Map<String, Integer> speciesCounts = new HashMap<>();
        
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            speciesCounts.put(animal.getSpeciesKey(), speciesCounts.getOrDefault(animal.getSpeciesKey(), 0) + 1);
        }

        for (Map.Entry<String, Integer> entry : speciesCounts.entrySet()) {
            String speciesKey = entry.getKey();
            int count = entry.getValue();
            
            // Pairing logic: if 2 or more animals of the same species
            if (count >= 2) {
                // In a more advanced version, we could call animal.reproduce() 
                // which would check energy etc. For now keeping existing logic.
                Animal baby = AnimalFactory.createAnimal(speciesKey);
                if (baby != null) {
                    cell.addAnimal(baby);
                }
            }
        }
    }

    private void reproducePlants(Cell cell) {
        List<Plant> currentPlants = cell.getPlants();
        List<Plant> newPlants = new ArrayList<>();
        
        for (Plant plant : currentPlants) {
            if (plant.isAlive()) {
                Plant baby = plant.reproduce(); // Polymorphic call
                if (baby != null) {
                    newPlants.add(baby);
                }
            }
        }
        
        for (Plant baby : newPlants) {
            cell.addPlant(baby);
        }
    }
}
