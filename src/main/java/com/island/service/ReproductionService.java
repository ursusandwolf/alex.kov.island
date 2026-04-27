package com.island.service;
import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.SpeciesKey;
import com.island.model.Cell;
import com.island.model.Island;
import static com.island.config.SimulationConstants.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;


public class ReproductionService extends AbstractService {
    private final AnimalFactory animalFactory;

    public ReproductionService(Island island, AnimalFactory animalFactory, ExecutorService executor) {
        super(island, executor);
        this.animalFactory = animalFactory;
    }

    @Override
    protected void processCell(Cell cell) {
        // Reproduce by species groups in the cell
        for (SpeciesKey speciesKey : animalFactory.getRegisteredSpecies()) {
            List<Animal> potentialMates = cell.getAnimalsBySpecies(speciesKey);
            
            // Need at least 2 to reproduce
            if (potentialMates.size() >= 2) {
                processSpeciesReproduction(cell, speciesKey, potentialMates);
            }
        }
    }

    private void processSpeciesReproduction(Cell cell, SpeciesKey key, List<Animal> mates) {
        int pairs = mates.size() / 2;
        int babiesCount = 0;

        for (int i = 0; i < pairs; i++) {
            Animal parent1 = mates.get(i * 2);
            Animal parent2 = mates.get(i * 2 + 1);

            if (parent1.canInitiateReproduction() && parent2.canInitiateReproduction()) {
                if (parent1.trySpendEnergyForReproduction() && parent2.trySpendEnergyForReproduction()) {
                    int offspring = calculateOffspringCount(parent1);
                    
                    // --- Endangered species bonus ---
                    if (isEndangered(parent1)) {
                        offspring = (int) (offspring * (1 + ENDANGERED_REPRO_BONUS_PERCENT / 100.0));
                        if (offspring == 0) offspring = 1; 
                    }

                    for (int j = 0; j < offspring; j++) {
                        animalFactory.createBaby(key).ifPresent(cell::addAnimal);
                        babiesCount++;
                    }
                }
            }
        }
    }

    private boolean isEndangered(Animal animal) {
        Island island = getIsland();
        int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
        int globalCapacity = (island.getWidth() * island.getHeight()) * animal.getMaxPerCell();
        return currentCount < globalCapacity * ENDANGERED_POPULATION_THRESHOLD;
    }

    private int calculateOffspringCount(Animal animal) {
        int baseOffspring;
        if (animal.getWeight() < WEIGHT_THRESHOLD_SMALL) {
            baseOffspring = OFFSPRING_SMALL_ANIMAL;
        } else {
            baseOffspring = OFFSPRING_LARGE_ANIMAL;
        }

        if (animal instanceof com.island.content.animals.herbivores.Herbivore) {
            baseOffspring += HERBIVORE_OFFSPRING_BONUS;
        }
        
        // Add randomization
        return ThreadLocalRandom.current().nextInt(1, baseOffspring + 1);
    }
}
