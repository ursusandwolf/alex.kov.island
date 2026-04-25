package com.island.service;
import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.model.Cell;
import com.island.model.Island;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class ReproductionService extends AbstractService {
    private final AnimalFactory animalFactory;

    public ReproductionService(Island island, AnimalFactory animalFactory, ExecutorService executor) {
        super(island, executor);
        this.animalFactory = animalFactory;
    }

    @Override
    protected void processCell(Cell cell) {
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
                    Animal baby = animalFactory.createAnimal(speciesKey);
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
        } else if (weight < 6.0) { 
            baseOffspring = 2;
        } else {
            baseOffspring = 1;
        }

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
                Plant baby = plant.reproduce(); 
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
