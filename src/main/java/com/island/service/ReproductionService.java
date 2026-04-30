package com.island.service;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal reproduction.
 */
public class ReproductionService extends AbstractService<Cell> {
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;

    public ReproductionService(SimulationWorld world, AnimalFactory animalFactory, 
                               SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    protected void processCell(Cell cell, int tickCount) {
        List<Animal> candidates = new ArrayList<>();
        cell.forEachAnimalSampled(com.island.config.SimulationConstants.REPRODUCTION_LOD_LIMIT, getRandom(), a -> {
            if (shouldAct(a, AnimalType.Action.REPRODUCE, tickCount)) {
                candidates.add(a);
            }
        });

        if (candidates.size() < 2) {
            return;
        }
        
        java.util.Set<Animal> alreadyMated = new java.util.HashSet<>();

        for (int i = 0; i < candidates.size(); i++) {
            Animal a1 = candidates.get(i);
            if (alreadyMated.contains(a1)) {
                continue;
            }

            for (int j = i + 1; j < candidates.size(); j++) {
                Animal a2 = candidates.get(j);
                if (alreadyMated.contains(a2)) {
                    continue;
                }

                if (a1.getAnimalType().equals(a2.getAnimalType())) {
                    if (tryReproduce(a1, a2, cell)) {
                        alreadyMated.add(a1);
                        alreadyMated.add(a2);
                        break;
                    }
                }
            }
        }
    }

    private boolean tryReproduce(Animal parent1, Animal parent2, Cell cell) {
        AnimalType type = parent1.getAnimalType();
        int maxOffspring = type.getMaxOffspring();
        
        // Endangered protection bonus: +1 to potential offspring
        if (protectionMap != null && protectionMap.containsKey(type.getSpeciesKey())) {
            maxOffspring++;
        }

        int count = getRandom().nextInt(0, maxOffspring + 1);
        if (count <= 0) {
            return false;
        }

        boolean success = false;
        for (int i = 0; i < count; i++) {
            Optional<Animal> baby = animalFactory.createBaby(type.getSpeciesKey());
            if (baby.isPresent()) {
                Animal babyAnimal = baby.get();
                if (cell.addAnimal(babyAnimal)) {
                    success = true;
                } else {
                    animalFactory.releaseAnimal(babyAnimal);
                    break; // Cell is full
                }
            }
        }

        if (success) {
            double costFactor = com.island.config.EnergyPolicy.REPRODUCTION_COST.getFactor();
            parent1.consumeEnergy(parent1.getMaxEnergy() * costFactor);
            parent2.consumeEnergy(parent2.getMaxEnergy() * costFactor);
        }
        return success;
    }
}
