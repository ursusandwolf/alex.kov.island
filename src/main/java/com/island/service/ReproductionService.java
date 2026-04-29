package com.island.service;

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
 * Service responsible for animal reproduction.
 */
public class ReproductionService extends AbstractService {
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
        if (node instanceof Cell cell) {
            List<Animal> potentialParents = cell.getAnimals();
            int size = potentialParents.size();
            if (size < 2) {
                return;
            }
            
            // LOD: Systematic sampling
            int limit = 50;
            int step = (size > limit) ? (size / limit + 1) : 1;

            java.util.Set<Animal> alreadyMated = new java.util.HashSet<>();

            for (int i = 0; i < size; i += step) {
                Animal a1 = potentialParents.get(i);
                if (alreadyMated.contains(a1) || !shouldReproduce(a1, tickCount)) {
                    continue;
                }

                // Try to find a mate in the same sampled set or nearby
                for (int j = i + step; j < size; j += step) {
                    Animal a2 = potentialParents.get(j);
                    if (alreadyMated.contains(a2) || !shouldReproduce(a2, tickCount)) {
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
    }

    private boolean shouldReproduce(Animal animal, int tickCount) {
        if (!animal.canInitiateReproduction()) {
            return false;
        }
        // Cold-blooded reproduce less frequently (every 4th tick)
        if (animal.getAnimalType().isColdBlooded()) {
            return (tickCount % 4 == 0);
        }
        return true;
    }

    private boolean tryReproduce(Animal parent1, Animal parent2, Cell cell) {
        AnimalType type = parent1.getAnimalType();
        
        // Dynamic reproduction chance based on size class
        double chance = switch (type.getSizeClass()) {
            case TINY -> 0.35;    // Mouse, Hamster
            case SMALL -> 0.25;   // Duck, Rabbit (Rabbit is NORMAL but tiny)
            case NORMAL -> 0.15;  // Fox, Eagle
            case MEDIUM -> 0.08;  // Wolf, Goat, Sheep
            case LARGE -> 0.04;   // Bear, Boar, Deer
            case HUGE -> 0.02;    // Buffalo, Horse
        };

        if (getRandom().nextDouble() < chance) {
            Optional<Animal> baby = animalFactory.createAnimal(type.getSpeciesKey());
            if (baby.isPresent()) {
                Animal babyAnimal = baby.get();
                if (cell.addAnimal(babyAnimal)) {
                    // Success! 
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
