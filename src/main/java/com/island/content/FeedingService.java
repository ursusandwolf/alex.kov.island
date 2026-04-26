package com.island.content;
import com.island.util.RandomUtils;import com.island.content.plants.*;
import com.island.content.animals.herbivores.Caterpillar;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import static com.island.config.SimulationConstants.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;


public class FeedingService implements Runnable {
    private final Island island;
    private final InteractionMatrix interactionMatrix;
    private final ExecutorService executor;
    private final SpeciesConfig speciesConfig = SpeciesConfig.getInstance();

    public FeedingService(Island island, InteractionMatrix interactionMatrix, ExecutorService executor) {
        this.island = island;
        this.interactionMatrix = interactionMatrix;
        this.executor = executor;
    }

    @Override
    public void run() {
        if (executor.isShutdown()) return;

        // Centralized: calculate protection map once per tick
        Map<String, Double> protectionMap = island.getProtectionMap(speciesConfig);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Chunk chunk : island.getChunks()) {
            tasks.add(() -> {
                for (Cell cell : chunk.getCells()) {
                    processCell(cell, protectionMap);
                }
                return null;
            });
        }
        try {
            if (!executor.isShutdown()) {
                executor.invokeAll(tasks);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Ignore shutdown races
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processCell(Cell cell, Map<String, Double> protectionMap) {
        List<Animal> predators;
        cell.getLock().lock();
        try {
            // Snapshot of predators for current tick processing
            predators = new ArrayList<>(cell.getAnimals());
        } finally {
            cell.getLock().unlock();
        }
        
        // Sorting by initiative (jittered)
        predators.sort((a, b) -> {
            double initiativeA = (a.getWeight() * 0.7 + a.getSpeed() * 0.3) * (0.8 + Math.random() * 0.4);
            double initiativeB = (b.getWeight() * 0.7 + b.getSpeed() * 0.3) * (0.8 + Math.random() * 0.4);
            return Double.compare(initiativeB, initiativeA);
        });

        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, island.getTickCount(), protectionMap);

        for (int i = 0; i < predators.size(); i++) {
            Animal predator = predators.get(i);
            if (predator.isAlive()) {
                tryEat(predator, cell, preyProvider, protectionMap);
            }
        }
    }

    private void tryEat(Animal predator, Cell cell, PreyProvider preyProvider, Map<String, Double> protectionMap) {
        // Upfront search cost is removed to avoid "absurd" losses on small prey.
        // Predators now pay per attempt based on prey size and difficulty.

        // Try hunting organisms (Animals and Smart Biomass like Caterpillar) provided by the mediator
        int attemptsInTick = 0;
        for (Organism prey : preyProvider.getPreyFor(predator)) {
            if (predator == prey || !prey.isAlive()) continue;
            attemptsInTick++;

            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (chance > 0) {
                // 1. Calculate costs and gains
                double preyWeight;
                if (prey instanceof Animal a) preyWeight = a.getWeight();
                else if (prey instanceof Caterpillar c) preyWeight = 0.01; 
                else preyWeight = 0;

                // Strike effort
                double strikeCost = Math.min(preyWeight * 0.1, predator.getMaxEnergy() * 0.005);
                
                // Chase cost (only for animals)
                double chaseCost = 0;
                if (prey instanceof Animal a) {
                    int speedDifference = a.getSpeed() - predator.getSpeed();
                    if (speedDifference > 0) {
                        chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
                    }
                }

                double totalEffort = strikeCost + chaseCost;
                
                // --- Hunt Fatigue Logic ---
                // After HUNT_FATIGUE_THRESHOLD attempts, cost increases by HUNT_FATIGUE_COST_MULTIPLIER every block
                if (attemptsInTick > HUNT_FATIGUE_THRESHOLD) {
                    int extraBlocks = (attemptsInTick - 1) / HUNT_FATIGUE_THRESHOLD;
                    totalEffort *= Math.pow(HUNT_FATIGUE_COST_MULTIPLIER, extraBlocks);
                }

                // Efficiency Check
                double expectedGain = preyWeight * (chance / 100.0);
                if (expectedGain < totalEffort && predator.getEnergyPercentage() > 40) continue;

                predator.consumeEnergy(totalEffort);
                if (!predator.isAlive()) return; // Stop if dead or energy exhausted

                // 2. Execution
                if (RandomUtils.checkChance(chance)) {
                    if (prey instanceof Animal a) {
                        if (cell.removeAnimal(a)) {
                            a.die();
                            predator.addEnergy(a.getWeight());
                            preyProvider.markAsEaten(a);
                            island.reportEatenAnimal();
                        }
                    } else if (prey instanceof Caterpillar c) {
                        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
                        double eaten = c.consumeBiomass(foodNeeded);
                        predator.addEnergy(eaten);
                        // For caterpillars, they stay in the cell but mass decreases
                    }
                    
                    // Check if satiated
                    if (predator.getCurrentEnergy() >= predator.getFoodForSaturation()) {
                        return; 
                    }
                } else if (prey instanceof Animal a) {
                    // Hunt failed! Prey escapes and hides.
                    preyProvider.markAsHiding(a);
                    a.consumeEnergy(a.getMaxEnergy() * ESCAPE_ENERGY_COST_PERCENT);
                }
            }
        }

        // --- Plant Feeding Logic (Traditional plants) ---
        String predatorKey = predator.getSpeciesKey();
        List<Plant> plants = cell.getPlants();
        if (plants.isEmpty()) return;

        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
        if (foodNeeded <= 0) return;

        // 1. Try eating Cabbage (priority for Rabbit, Goat, Duck)
        if (predatorKey.equals("rabbit") || predatorKey.equals("goat") || predatorKey.equals("duck")) {
            for (int i = 0; i < plants.size(); i++) {
                Plant plant = plants.get(i);
                if (plant instanceof Cabbage && plant.isAlive()) {
                    // Check protection
                    Double hideChance = protectionMap.get(plant.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) return; // Satiated
                }
            }
        }

        // 2. Try eating Grass
        int plantChance = interactionMatrix.getChance(predatorKey, "Plant");
        if (plantChance > 0) {
            for (int i = 0; i < plants.size(); i++) {
                Plant plant = plants.get(i);
                if (plant instanceof Grass && plant.isAlive()) {
                    // Check protection
                    Double hideChance = protectionMap.get(plant.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) return; // Satiated
                }
            }
        }
    }
}
