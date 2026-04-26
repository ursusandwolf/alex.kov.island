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
            executor.invokeAll(tasks);
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

        // Try hunting animals provided by the mediator
        for (Animal prey : preyProvider.getPreyFor(predator)) {
            if (predator == prey || !prey.isAlive() || prey.isProtected(island.getTickCount())) continue;

            int chance = interactionMatrix.getChance(predator.getSpeciesKey(), prey.getSpeciesKey());
            if (chance > 0) {
                // 1. Strike effort: Proportional to prey size, but capped.
                // Ensures that hunting a mouse doesn't cost a bear more than the mouse weighs.
                double strikeCost = Math.min(prey.getWeight() * 0.1, predator.getMaxEnergy() * 0.005);
                
                // 2. Relative speed logic (Chase cost)
                double chaseCost = 0;
                int speedDifference = prey.getSpeed() - predator.getSpeed();
                if (speedDifference > 0) {
                    chaseCost = predator.getMaxEnergy() * (speedDifference * PREY_RELATIVE_SPEED_HUNT_COST_STEP_PERCENT);
                }

                double totalEffort = strikeCost + chaseCost;

                // 3. Efficiency Check: Don't hunt if expected gain < effort
                double expectedGain = prey.getWeight() * (chance / 100.0);
                if (expectedGain < totalEffort && predator.getEnergyPercentage() > 40) {
                    continue; // Skip unprofitable prey unless desperate
                }

                predator.consumeEnergy(totalEffort);
                if (!predator.isAlive()) return;

                if (RandomUtils.checkChance(chance)) {
                    if (cell.removeAnimal(prey)) {
                        prey.die();
                        predator.addEnergy(prey.getWeight());
                        preyProvider.markAsEaten(prey);
                        
                        // Report to global stats
                        if (!prey.getSpeciesKey().equals("caterpillar")) {
                            island.reportEatenAnimal();
                        }
                        
                        // Check if satiated
                        if (predator.getCurrentEnergy() >= predator.getFoodForSaturation()) {
                            return; 
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

        // 2. Try eating Grass (for anyone who can eat "Plant" in matrix)
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

        // 3. Try eating Caterpillar Biomass (for Fox, Boar, etc.)
        int caterpillarChance = interactionMatrix.getChance(predatorKey, "caterpillar");
        if (caterpillarChance > 0) {
            for (int i = 0; i < plants.size(); i++) {
                Plant p = plants.get(i);
                if (p instanceof Caterpillar && p.isAlive()) {
                    // Check protection (Smart Biomass hiding)
                    Double hideChance = protectionMap.get(p.getSpeciesKey());
                    if (hideChance != null && Math.random() < hideChance) continue;

                    double eaten = p.consumeBiomass(foodNeeded);
                    predator.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) return;
                }
            }
        }
    }
}
