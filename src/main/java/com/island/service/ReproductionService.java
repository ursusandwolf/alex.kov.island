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
        // Group animals by species
        Map<String, List<Animal>> speciesGroups = new HashMap<>();
        for (Animal animal : animals) {
            if (animal.isAlive()) {
                speciesGroups.computeIfAbsent(animal.getSpeciesKey(), k -> new ArrayList<>()).add(animal);
            }
        }

        for (Map.Entry<String, List<Animal>> entry : speciesGroups.entrySet()) {
            String speciesKey = entry.getKey();
            List<Animal> group = entry.getValue();
            int count = group.size();
            
            if (count >= 2) {
                int pairs = count / 2;
                Animal representative = group.get(0);
                int offspringPerPair = calculateOffspringCount(representative);
                int totalOffspring = pairs * offspringPerPair;
                
                for (int i = 0; i < totalOffspring; i++) {
                    Animal baby = AnimalFactory.createAnimal(speciesKey);
                    if (baby != null) {
                        cell.addAnimal(baby);
                    }
                }
            }
        }
    }

    private int calculateOffspringCount(Animal animal) {
        double weight = animal.getWeight();
        int baseOffspring;
        
        if (animal.getSpeciesKey().equals("caterpillar")) {
            baseOffspring = 4;
        } else if (weight < 6.0) { // All small animals (including mice < 1kg and rabbits 1-6kg) get 2 base
            baseOffspring = 2;
        } else {
            baseOffspring = 1;
        }

        // Additional +1 for herbivores
        if (animal instanceof com.island.content.animals.herbivores.Herbivore) {
            baseOffspring += 1;
        }
        
        return baseOffspring;
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
