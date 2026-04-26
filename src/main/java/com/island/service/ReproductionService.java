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
        
        Island island = cell.getIsland();
        int islandArea = island.getWidth() * island.getHeight();

        for (Animal animal : animals) {
            String key = animal.getSpeciesKey();
            int currentCount = island.getSpeciesCount(key);
            int globalCapacity = islandArea * animal.getMaxPerCell();
            
            // Check for Red Book Reproduction Bonus
            double threshold = globalCapacity * ENDANGERED_POPULATION_THRESHOLD;
            double requiredPercent = REPRODUCTION_MIN_ENERGY_PERCENT;
            if (currentCount > 0 && currentCount < threshold) {
                requiredPercent -= ENDANGERED_REPRO_BONUS_PERCENT; // 60 -> 40
            }

            if (animal.isAlive() && animal.getEnergyPercentage() >= requiredPercent) {
                readyGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(animal);
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
        double maxEnergy = p1.getMaxEnergy();
        int maxOffspring = calculateMaxOffspringCount(p1);
        
        int offspringCount = 0;
        double survivalFloor = maxEnergy * 0.4 - 0.005;

        for (int k = maxOffspring; k >= 1; k--) {
            double requiredTotal = (2 + k) * survivalFloor;
            if (k == 1) requiredTotal = Math.max(requiredTotal, maxEnergy * 1.5 - 0.05); 
            
            if (totalEnergy >= requiredTotal) {
                offspringCount = k;
                break;
            }
        }

        if (offspringCount > 0) {
            double finalEnergyPerHead = totalEnergy / (2 + offspringCount);
            
            p1.setEnergy(finalEnergyPerHead);
            p2.setEnergy(finalEnergyPerHead);
            
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
