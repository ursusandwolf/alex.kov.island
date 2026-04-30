package com.island.service;

import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.config.EnergyPolicy;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for animal reproduction using integer-based arithmetic.
 */
public class ReproductionService extends AbstractService<SimulationNode> {
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;

    public ReproductionService(SimulationWorld world, AnimalFactory animalFactory, 
                               SpeciesRegistry speciesRegistry, ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
    }

    @Override
    protected void processCell(SimulationNode node, int tickCount) {
        List<Animal> candidates = new ArrayList<>();
        int totalAnimalsInCell = node.getLivingEntities().size(); 
        int limit = com.island.config.SimulationConstants.REPRODUCTION_LOD_LIMIT;
        
        node.forEachAnimalSampled(limit, getRandom(), a -> {
            if (shouldAct(a, AnimalType.Action.REPRODUCE, tickCount)) {
                candidates.add(a);
            }
        });

        if (candidates.size() < 2) {
            return;
        }
        
        int samplingScale = (totalAnimalsInCell > limit) ? (totalAnimalsInCell / limit) : 1;
        java.util.Set<Animal> alreadyMated = new java.util.HashSet<>();

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
                    // Check reproduction chance from AnimalType
                    int chance = a1.getAnimalType().getReproductionChance();
                    if (getRandom().nextInt(0, 100) < chance) {
                        if (tryReproduceScaled(a1, a2, node, samplingScale)) {
                            alreadyMated.add(a1);
                            alreadyMated.add(a2);
                            break;
                        }
                    }
                }
            }
        }
    }

    private boolean tryReproduceScaled(Animal parent1, Animal parent2, SimulationNode node, int scale) {
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
            int costBP = com.island.config.EnergyPolicy.REPRODUCTION_COST_BP.getBasisPoints();
            parent1.consumeEnergy((parent1.getMaxEnergy() * costBP) / com.island.config.SimulationConstants.SCALE_10K);
            parent2.consumeEnergy((parent2.getMaxEnergy() * costBP) / com.island.config.SimulationConstants.SCALE_10K);
        }
        return success;
    }

    private boolean tryReproduce(Animal parent1, Animal parent2, SimulationNode node) {
        return tryReproduceScaled(parent1, parent2, node, 1);
    }
}
