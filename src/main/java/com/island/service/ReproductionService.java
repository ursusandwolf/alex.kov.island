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
    protected void processCell(SimulationNode node) {
        if (node instanceof Cell cell) {
            List<Animal> potentialParents = new ArrayList<>(cell.getAnimals());
            // Use deterministic shuffle if needed, or just random
            java.util.Collections.shuffle(potentialParents, new java.util.Random(getRandom().nextLong()));

            java.util.Set<Animal> alreadyMated = new java.util.HashSet<>();

            for (Animal a1 : potentialParents) {
                if (alreadyMated.contains(a1) || !a1.canInitiateReproduction()) {
                    continue;
                }

                for (Animal a2 : potentialParents) {
                    if (a1 == a2 || alreadyMated.contains(a2) || !a2.canInitiateReproduction()) {
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

    private boolean tryReproduce(Animal parent1, Animal parent2, Cell cell) {
        AnimalType type = parent1.getAnimalType();
        double chance = 1.0; // Guaranteed for testing/balancing

        if (getRandom().nextDouble() < chance) {
            Optional<Animal> baby = animalFactory.createAnimal(type.getSpeciesKey());
            if (baby.isPresent()) {
                Animal babyAnimal = baby.get();
                if (cell.addAnimal(babyAnimal)) {
                    // Success! 
                    parent1.consumeEnergy(parent1.getMaxEnergy() * 0.1); // Small cost
                    parent2.consumeEnergy(parent2.getMaxEnergy() * 0.1);
                    return true;
                }
            }
        }
        return false;
    }
}
