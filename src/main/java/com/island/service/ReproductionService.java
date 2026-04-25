package com.island.service;
import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.model.Cell;
import com.island.model.Island;
import static com.island.config.SimulationConstants.*;

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
        // Reproduction of Animals (requires pairs)
        reproduceAnimals(cell);
    }

    private void reproduceAnimals(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        // Group animals by species AND check for reproduction health
        Map<String, List<Animal>> readyGroups = new HashMap<>();
        for (Animal animal : animals) {
            // isAlive() check and trySpendEnergyForReproduction() which checks thresholds and deducts energy
            if (animal.isAlive() && animal.trySpendEnergyForReproduction()) {
                readyGroups.computeIfAbsent(animal.getSpeciesKey(), k -> new ArrayList<>()).add(animal);
            }
        }

        for (Map.Entry<String, List<Animal>> entry : readyGroups.entrySet()) {
            String speciesKey = entry.getKey();
            List<Animal> group = entry.getValue();
            int readyCount = group.size();
            
            if (readyCount >= 2) {
                int pairs = readyCount / 2;
                Animal representative = group.get(0);
                int offspringPerPair = calculateOffspringCount(representative);
                int totalOffspring = pairs * offspringPerPair;
                
                for (int i = 0; i < totalOffspring; i++) {
                    Animal baby = animalFactory.createBaby(speciesKey);
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
            baseOffspring = OFFSPRING_INSECT;
        } else if (weight < WEIGHT_THRESHOLD_SMALL) { 
            baseOffspring = OFFSPRING_SMALL_ANIMAL;
        } else {
            baseOffspring = OFFSPRING_LARGE_ANIMAL;
        }

        if (animal instanceof com.island.content.animals.herbivores.Herbivore) {
            baseOffspring += HERBIVORE_OFFSPRING_BONUS;
        }
        
        return baseOffspring;
    }
}
