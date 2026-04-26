package com.island.content;
import com.island.util.RandomUtils;import com.island.content.plants.*;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import static com.island.config.SimulationConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;


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
        List<Animal> predators = new ArrayList<>(cell.getAnimals());
        
        // Sorting by initiative: (Weight * 0.7 + Speed * 0.3) * Random(0.8..1.2)
        // This gives chance for faster/lighter predators to act before heavy ones.
        predators.sort((a, b) -> {
            double initiativeA = (a.getWeight() * 0.7 + a.getSpeed() * 0.3) * (0.8 + Math.random() * 0.4);
            double initiativeB = (b.getWeight() * 0.7 + b.getSpeed() * 0.3) * (0.8 + Math.random() * 0.4);
            return Double.compare(initiativeB, initiativeA);
        });

        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, island.getTickCount());

        for (Animal predator : predators) {
            if (predator.isAlive()) {
                tryEat(predator, cell, preyProvider);
            }
        }
    }

    private void tryEat(Animal predator, Cell cell, PreyProvider preyProvider) {
        // Base hunting cost
        double huntEffortCost = predator.getMaxEnergy() * (BASE_HUNT_COST_PERCENT 
            + (predator.getSpeed() * PREDATOR_SPEED_HUNT_COST_STEP_PERCENT));
        predator.consumeEnergy(huntEffortCost);

        if (!predator.isAlive()) return;

        // Try hunting animals provided by the mediator
        for (Animal prey : preyProvider.getPreyFor(predator)) {
            if (predator == prey || !prey.isAlive() || prey.isProtected(island.getTickCount())) continue;

            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (chance > 0) {
                // Relative speed logic
                int speedDifference = prey.getSpeed() - predator.getSpeed();
                if (speedDifference > 0) {
                    double chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
                    predator.consumeEnergy(chaseCost);
                    if (!predator.isAlive()) return;
                }

                if (RandomUtils.checkChance(chance)) {
                    if (cell.removeAnimal(prey)) {
                        prey.die();
                        predator.addEnergy(prey.getWeight());
                        preyProvider.markAsEaten(prey);
                        
                        // Check if satiated
                        if (predator.getCurrentEnergy() >= predator.getFoodForSaturation()) {
                            return; // Predator is full, stop hunting
                        }
                    }
                } else {
                    // Hunt failed! Prey escapes and hides.
                    preyProvider.markAsHiding(prey);
                    double escapeCost = prey.getMaxEnergy() * ESCAPE_ENERGY_COST_PERCENT; 
                    prey.consumeEnergy(escapeCost);
                }
            }
        }

        // --- Plant Feeding Logic ---
        // (Plants are simpler and don't yet use the mediator in this version)
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
