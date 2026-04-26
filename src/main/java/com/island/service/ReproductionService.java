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
        Map<String, List<Animal>> readyGroups = new HashMap<>();
        
        for (Animal animal : animals) {
            if (animal.canInitiateReproduction()) {
                readyGroups.computeIfAbsent(animal.getSpeciesKey(), k -> new ArrayList<>()).add(animal);
            }
        }

        for (Map.Entry<String, List<Animal>> entry : readyGroups.entrySet()) {
            String speciesKey = entry.getKey();
            List<Animal> group = entry.getValue();
            
            // Pair them up
            for (int i = 0; i + 1 < group.size(); i += 2) {
                Animal parent1 = group.get(i);
                Animal parent2 = group.get(i + 1);
                
                processPair(cell, parent1, parent2, speciesKey);
            }
        }
    }

    private void processPair(Cell cell, Animal p1, Animal p2, String speciesKey) {
        double totalEnergy = p1.getCurrentEnergy() + p2.getCurrentEnergy();
        double maxEnergyPerHead = p1.getMaxEnergy();
        int maxOffspring = calculateMaxOffspringCount(p1);
        
        // Find max children we can afford while keeping everyone above 40% energy
        // E_new = E_total / (2 + k) >= 0.4 * E_max
        int offspringCount = 0;
        for (int k = maxOffspring; k >= 1; k--) {
            double energyPerIndividual = totalEnergy / (2 + k);
            if (energyPerIndividual >= maxEnergyPerHead * 0.4) {
                offspringCount = k;
                break;
            }
        }

        // Special case: 150 energy total for 1 offspring (as per user requirement)
        // If the formula above didn't yield offspring but we have enough for 1 child at 50%
        if (offspringCount == 0 && totalEnergy >= maxEnergyPerHead * 1.5) {
            offspringCount = 1;
        }

        if (offspringCount > 0) {
            double finalEnergyPerHead = totalEnergy / (2 + offspringCount);
            
            // Update parents
            p1.setEnergy(finalEnergyPerHead);
            p2.setEnergy(finalEnergyPerHead);
            
            // Create babies
            for (int j = 0; j < offspringCount; j++) {
                Animal baby = animalFactory.createAnimalWithEnergy(speciesKey, finalEnergyPerHead);
                if (baby != null) {
                    cell.addAnimal(baby);
                }
            }
        }
    }

    private int calculateMaxOffspringCount(Animal animal) {
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
