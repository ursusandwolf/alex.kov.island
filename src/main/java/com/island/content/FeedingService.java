package com.island.content;
import com.island.util.RandomUtils;import com.island.content.plants.*;
import com.island.content.DeathCause;
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
    private final HuntingStrategy huntingStrategy;

    public FeedingService(Island island, InteractionMatrix interactionMatrix, ExecutorService executor) {
        this.island = island;
        this.interactionMatrix = interactionMatrix;
        this.executor = executor;
        this.huntingStrategy = new DefaultHuntingStrategy(interactionMatrix);
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
            if (predator.isAlive() && !predator.isHibernating()) {
                tryEat(predator, cell, preyProvider, protectionMap);
            }
        }
    }

    private void tryEat(Animal predator, Cell cell, PreyProvider preyProvider, Map<String, Double> protectionMap) {
        // Try hunting organisms (Animals and Smart Biomass like Caterpillar) provided by the mediator
        int attemptsInTick = 0;
        for (Organism prey : preyProvider.getPreyFor(predator)) {
            if (predator == prey || !prey.isAlive()) continue;
            attemptsInTick++;

            double successRate = huntingStrategy.calculateSuccessRate(predator, prey);
            if (successRate > 0) {
                double totalEffort = huntingStrategy.calculateHuntCost(predator, prey);
                
                // --- Hunt Fatigue Logic ---
                if (attemptsInTick > HUNT_FATIGUE_THRESHOLD) {
                    int extraBlocks = (attemptsInTick - 1) / HUNT_FATIGUE_THRESHOLD;
                    totalEffort *= Math.pow(HUNT_FATIGUE_COST_MULTIPLIER, extraBlocks);
                }

                // --- Strict Efficiency (ROI) Check ---
                if (!huntingStrategy.isWorthHunting(predator, prey, successRate, totalEffort)) {
                    continue; // "Not worth the risk": skip this prey
                }

                predator.consumeEnergy(totalEffort);
                if (!predator.isAlive()) {
                    island.reportDeath(predator.getSpeciesKey(), DeathCause.HUNGER);
                    return; // Stop if dead or energy exhausted
                }

                // 2. Execution with atomic check-and-consume
                boolean success = false;
                if (RandomUtils.nextDouble() < successRate) {
                    cell.getLock().lock();
                    try {
                        if (prey instanceof Animal a) {
                            if (a.isAlive() && cell.removeAnimal(a)) {
                                a.die();
                                predator.addEnergy(a.getWeight());
                                preyProvider.markAsEaten(a);
                                island.reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                                success = true;
                            }
                        } else if (prey instanceof Caterpillar c) {
                            if (c.isAlive()) {
                                double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
                                double eaten = c.consumeBiomass(foodNeeded);
                                predator.addEnergy(eaten);
                                success = true;
                            }
                        }
                    } finally {
                        cell.getLock().unlock();
                    }
                    
                    // Check if satiated
                    if (success && predator.getCurrentEnergy() >= predator.getFoodForSaturation()) {
                        return; 
                    }
                } 
                
                if (!success && prey instanceof Animal a) {
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

        // Any animal that can eat "plant" in the matrix can eat Cabbage and Grass
        int canEatPlants = interactionMatrix.getChance(predatorKey, "plant");
        if (canEatPlants > 0) {
            // 1. Try eating Cabbage first (higher energy density)
            for (int i = 0; i < plants.size(); i++) {
                Plant plant = plants.get(i);
                if (plant instanceof Cabbage && plant.isAlive()) {
                    Double hideChance = protectionMap.get(plant.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) return; 
                }
            }

            // 2. Try eating Grass 
            for (int i = 0; i < plants.size(); i++) {
                Plant plant = plants.get(i);
                if (plant instanceof Grass && plant.isAlive()) {
                    Double hideChance = protectionMap.get(plant.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    double eaten = plant.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) return; 
                }
            }
        }
    }
}
