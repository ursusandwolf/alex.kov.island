package com.island.service;

import com.island.content.Animal;
import com.island.content.Plant;
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
        for (Animal animal : animals) {
            if (!animal.isAlive()) continue;
            tryEat(animal, cell);
        }
    }

    private void tryEat(Animal predator, Cell cell) {
        // Base hunting cost: base % + predator speed step %
        double huntEffortCost = predator.getMaxEnergy() * (BASE_HUNT_COST_PERCENT 
            + (predator.getSpeed() * PREDATOR_SPEED_HUNT_COST_STEP_PERCENT));
        predator.consumeEnergy(huntEffortCost);

        if (!predator.isAlive()) return;

        // Try hunting animals
        List<Animal> potentialPrey = cell.getAnimals();
        for (Animal prey : potentialPrey) {
            if (predator == prey || !prey.isAlive()) continue;

            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (chance > 0) {
                // Relative speed logic: if predator is slower than prey, additional cost is applied
                int speedDifference = prey.getSpeed() - predator.getSpeed();
                if (speedDifference > 0) {
                    double chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
                    predator.consumeEnergy(chaseCost);

                    if (!predator.isAlive()) return; // Predator died from exhaustion
                }

                if (ThreadLocalRandom.current().nextInt(100) < chance) {
                    // Successful hunt
                    predator.addEnergy(prey.getWeight());
                    cell.removeAnimal(prey);
                    return;
                }
            }
        }

        // If no animal caught, try eating plants (for herbivores/omnivores)
        int plantChance = interactionMatrix.getChance(predator.getSpeciesKey(), "Plant");
        if (plantChance > 0) {
            List<Plant> plants = cell.getPlants();
            if (!plants.isEmpty()) {
                Plant plant = plants.get(0);
                if (plant.isAlive()) {
                    predator.addEnergy(1.0); // 1kg of plant energy
                    cell.removePlant(plant);
                }
            }
        }
    }
}
