package com.island.service;

import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.config.EnergyPolicy;
import com.island.config.SimulationConstants;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.NatureWorld;
import com.island.content.Organism;
import com.island.content.SpeciesRegistry;
import com.island.engine.SimulationNode;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal reproduction using integer-based arithmetic.
 */
public class ReproductionService extends AbstractService {
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;

    public ReproductionService(NatureWorld world, AnimalFactory animalFactory, 
                               SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    public void processCell(Cell cell, int tickCount) {
        List<Animal> candidates = new ArrayList<>();
        int totalAnimalsInCell = cell.getAnimalCount(); 
        int limit = SimulationConstants.REPRODUCTION_LOD_LIMIT;
        
        cell.forEachAnimalSampled(limit, getRandom(), a -> {
            if (shouldAct(a, AnimalType.Action.REPRODUCE, tickCount)) {
                candidates.add(a);
            }
        });

        if (candidates.size() < 2) {
            return;
        }
        
        int samplingScale = (totalAnimalsInCell > limit) ? (totalAnimalsInCell / limit) : 1;
        Set<Animal> alreadyMated = new HashSet<>();

        for (int i = 0; i < candidates.size(); i++) {
            Animal a1 = candidates.get(i);
            if (alreadyMated.contains(a1) || !a1.canInitiateReproduction()) {
                continue;
            }

            for (int j = i + 1; j < candidates.size(); j++) {
                Animal a2 = candidates.get(j);
                if (alreadyMated.contains(a2) || !a2.canInitiateReproduction()) {
                    continue;
                }

                if (a1.getAnimalType().equals(a2.getAnimalType())) {
                    int chance = a1.getAnimalType().getReproductionChance();
                    if (getRandom().nextInt(0, 100) < chance) {
                        if (tryReproduceScaled(a1, a2, cell, samplingScale)) {
                            alreadyMated.add(a1);
                            alreadyMated.add(a2);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean tryReproduceScaled(Animal parent1, Animal parent2, Cell node, int scale) {
        AnimalType type = parent1.getAnimalType();
        int baseMaxOffspring = type.getMaxOffspring();
        boolean isEndangered = protectionMap != null && protectionMap.containsKey(type.getSpeciesKey());
        
        if (isEndangered) {
            baseMaxOffspring += 2;
        }

        int countPerPair = getRandom().nextInt(0, baseMaxOffspring + 1);
        if (isEndangered && countPerPair == 0) {
            countPerPair = 1;
        }

        if (countPerPair <= 0) {
            return false;
        }

        int totalCount = countPerPair * scale;
        boolean success = false;
        for (int i = 0; i < totalCount; i++) {
            Optional<Animal> baby = animalFactory.createBaby(type.getSpeciesKey());
            if (baby.isPresent()) {
                Animal babyAnimal = baby.get();
                if (node.addEntity(babyAnimal)) {
                    success = true;
                } else {
                    animalFactory.releaseAnimal(babyAnimal);
                    break; // Node is full
                }
            }
        }

        if (success) {
            int costBP = EnergyPolicy.REPRODUCTION_COST_BP.getBasisPoints();
            parent1.consumeEnergy((parent1.getMaxEnergy() * costBP) / SimulationConstants.SCALE_10K);
            parent2.consumeEnergy((parent2.getMaxEnergy() * costBP) / SimulationConstants.SCALE_10K);
        }
        return success;
    }
}
