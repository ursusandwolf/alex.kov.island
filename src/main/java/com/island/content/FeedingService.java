package com.island.content;

import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.engine.InteractionMatrix;
import static com.island.config.SimulationConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

public class FeedingService implements Runnable {
    private final Island island;
    private final InteractionMatrix interactionMatrix;
    private final ExecutorService executor;

    public FeedingService(Island island, InteractionMatrix interactionMatrix, ExecutorService executor) {
        this.island = island;
        this.interactionMatrix = interactionMatrix;
        this.executor = executor;
    }

    @Override
    public void run() {
        List<Callable<Void>> tasks = new ArrayList<>();
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    processCell(cell);
                }
                return null;
            });
        }
        try {
            executor.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processCell(Cell cell) {
        List<Animal> animals = cell.getAnimals();
        
        // Trophic Hierarchy Sorting:
        // 1. Predators of animals (isAnimalPredator == true) have higher priority.
        // 2. Heavier animals have higher priority within their group.
        animals.sort((a, b) -> {
            if (a.isAnimalPredator() != b.isAnimalPredator()) {
                return a.isAnimalPredator() ? -1 : 1; // Predators first
            }
            return Double.compare(b.getWeight(), a.getWeight()); // Then by weight descending
        });

        for (Animal animal : animals) {
            if (animal.isAlive()) {
                tryEat(animal, cell);
            }
        }
    }

    private void tryEat(Animal predator, Cell cell) {
        // Base hunting cost
        double huntEffortCost = predator.getMaxEnergy() * (BASE_HUNT_COST_PERCENT 
            + (predator.getSpeed() * PREDATOR_SPEED_HUNT_COST_STEP_PERCENT));
        predator.consumeEnergy(huntEffortCost);

        if (!predator.isAlive()) return;

        // Try hunting animals
        List<Animal> potentialPrey = cell.getAnimals();
        for (Animal prey : potentialPrey) {
            if (predator == prey || !prey.isAlive()) continue;

            // Protection check: hides if escaped before
            if (prey.isProtected(island.getTickCount())) {
                continue;
            }

            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (chance > 0) {
                // Relative speed logic
                int speedDifference = prey.getSpeed() - predator.getSpeed();
                if (speedDifference > 0) {
                    double chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
                    predator.consumeEnergy(chaseCost);
                    if (!predator.isAlive()) return;
                }

                if (ThreadLocalRandom.current().nextInt(100) < chance) {
                    // ATOMIC CHECK: Successful hunt ONLY if we can actually remove the prey from the cell.
                    if (cell.removeAnimal(prey)) {
                        prey.die(); // Ensure prey is marked as dead
                        predator.addEnergy(prey.getWeight());
                        return;
                    }
                } else {
                    // Hunt failed! Prey escapes and HIDES for the rest of the tick.
                    prey.setHiding(true);
                    double escapeCost = prey.getMaxEnergy() * 0.05; // 5% energy to escape
                    prey.consumeEnergy(escapeCost);
                }
            }
        }

        // --- Plant Feeding Logic ---
        String predatorKey = predator.getSpeciesKey();
        List<Plant> plants = cell.getPlants();
        if (plants.isEmpty()) return;

        double foodNeeded = predator.getFoodForSaturation() - (predator.getCurrentEnergy());
        if (foodNeeded <= 0) return;

        // 1. Try eating Cabbage (priority for Rabbit, Goat, Duck)
        if (predatorKey.equals("rabbit") || predatorKey.equals("goat") || predatorKey.equals("duck")) {
            for (Plant plant : plants) {
                if (plant instanceof Cabbage && plant.isAlive()) {
                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    return; // Satiated or plant exhausted
                }
            }
        }

        // 2. Try eating Grass (for anyone who can eat "Plant" in matrix)
        int plantChance = interactionMatrix.getChance(predatorKey, "Plant");
        if (plantChance > 0) {
            for (Plant plant : plants) {
                if (plant instanceof Grass && plant.isAlive()) {
                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    return;
                }
            }
        }
    }
}
