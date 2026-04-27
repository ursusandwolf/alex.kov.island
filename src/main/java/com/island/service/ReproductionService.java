package com.island.service;

import static com.island.config.SimulationConstants.ENDANGERED_POPULATION_THRESHOLD;
import static com.island.config.SimulationConstants.ENDANGERED_REPRO_BONUS_PERCENT;
import static com.island.config.SimulationConstants.HERBIVORE_OFFSPRING_BONUS;

import com.island.config.EnergyPolicy;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SizeClass;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.model.Cell;
import com.island.model.Island;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for animal reproduction.
 */
public class ReproductionService extends AbstractService {
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;

    public ReproductionService(Island island, AnimalFactory animalFactory, 
                               SpeciesRegistry speciesRegistry, ExecutorService executor) {
        super(island, executor);
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    protected void processCell(Cell cell) {
        // Reproduce by species groups in the cell
        for (SpeciesKey speciesKey : animalFactory.getRegisteredSpecies()) {
            AnimalType type = speciesRegistry.getAnimalType(speciesKey).orElse(null);
            List<Animal> potentialMates = cell.getAnimalsByType(type);
            
            // Need at least 2 to reproduce
            if (potentialMates.size() >= 2) {
                processSpeciesReproduction(cell, speciesKey, potentialMates);
            }
        }
    }

    private void processSpeciesReproduction(Cell cell, SpeciesKey key, List<Animal> mates) {
        int pairs = mates.size() / 2;

        for (int i = 0; i < pairs; i++) {
            Animal parent1 = mates.get(i * 2);
            Animal parent2 = mates.get(i * 2 + 1);

            if (parent1.canInitiateReproduction() && parent2.canInitiateReproduction()) {
                if (trySpendEnergyForReproduction(parent1) && trySpendEnergyForReproduction(parent2)) {
                    int offspring = calculateOffspringCount(parent1);
                    
                    // --- Endangered species bonus ---
                    if (isEndangered(parent1)) {
                        offspring = (int) (offspring * (1 + ENDANGERED_REPRO_BONUS_PERCENT / 100.0));
                        if (offspring == 0) {
                            offspring = 1; 
                        }
                    }

                    for (int j = 0; j < offspring; j++) {
                        animalFactory.createBaby(key).ifPresent(cell::addAnimal);
                    }
                }
            }
        }
    }

    private boolean trySpendEnergyForReproduction(Animal animal) {
        if (animal.getEnergyPercentage() < EnergyPolicy.REPRODUCTION_MIN.getPercent()) {
            return false;
        }
        double cost = animal.getMaxEnergy() * EnergyPolicy.REPRODUCTION_COST.getFactor();
        if (animal.getCurrentEnergy() > cost) {
            animal.consumeEnergy(cost);
            return true;
        }
        return false;
    }

    private boolean isEndangered(Animal animal) {
        Island island = getIsland();
        int currentCount = island.getSpeciesCount(animal.getSpeciesKey());
        int globalCapacity = (island.getWidth() * island.getHeight()) * animal.getMaxPerCell();
        return currentCount < globalCapacity * ENDANGERED_POPULATION_THRESHOLD;
    }

    private int calculateOffspringCount(Animal animal) {
        SizeClass sizeClass = SizeClass.fromWeight(animal.getWeight());
        int baseOffspring = sizeClass.getOffspringCount();

        baseOffspring += animal.getOffspringBonus();
        
        // Add randomization
        return ThreadLocalRandom.current().nextInt(1, baseOffspring + 1);
    }
}
