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
        List<Animal> potentialParents = cell.getAnimals();
        if (potentialParents.size() < 2) {
            return;
        }
        
        java.util.Set<Animal> alreadyMated = new java.util.HashSet<>();

        forEachSampled(potentialParents, com.island.config.SimulationConstants.REPRODUCTION_LOD_LIMIT, a1 -> {
            if (alreadyMated.contains(a1) || !shouldAct(a1, AnimalType.Action.REPRODUCE, tickCount)) {
                return;
            }

            // Try to find a mate
            for (Animal a2 : potentialParents) {
                if (a1 == a2 || alreadyMated.contains(a2) || !shouldAct(a2, AnimalType.Action.REPRODUCE, tickCount)) {
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
        });
    }

    private boolean tryReproduce(Animal parent1, Animal parent2, Cell cell) {
        AnimalType type = parent1.getAnimalType();
        double chance = type.getReproductionChance();
        
        // Endangered protection bonus
        if (protectionMap != null && protectionMap.containsKey(type.getSpeciesKey())) {
            chance *= (1.0 + com.island.config.SimulationConstants.ENDANGERED_REPRO_BONUS_PERCENT / 100.0);
        }

        if (getRandom().nextDouble() < chance) {
            Optional<Animal> baby = animalFactory.createAnimal(type.getSpeciesKey());
            if (baby.isPresent()) {
                Animal babyAnimal = baby.get();
                if (cell.addAnimal(babyAnimal)) {
                    double costFactor = com.island.config.EnergyPolicy.REPRODUCTION_COST.getFactor();
                    parent1.consumeEnergy(parent1.getMaxEnergy() * costFactor);
                    parent2.consumeEnergy(parent2.getMaxEnergy() * costFactor);
                    return true;
                } else {
                    animalFactory.releaseAnimal(babyAnimal);
                }
            }
        }
        return false;
    }
}
