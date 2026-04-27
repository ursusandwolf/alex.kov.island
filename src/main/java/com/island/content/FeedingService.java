package com.island.content;

import static com.island.config.SimulationConstants.ESCAPE_ENERGY_COST_PERCENT;
import static com.island.config.SimulationConstants.HUNT_FATIGUE_COST_MULTIPLIER;
import static com.island.config.SimulationConstants.HUNT_FATIGUE_THRESHOLD;

import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.plants.Cabbage;
import com.island.content.plants.Grass;
import com.island.content.plants.Plant;
import com.island.model.Cell;
import com.island.model.Chunk;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for feeding logic of all animals.
 */
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
        if (executor.isShutdown()) {
            return;
        }

        // Centralized: calculate protection map once per tick
        Map<SpeciesKey, Double> protectionMap = island.getProtectionMap(speciesConfig);

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

    private void processCell(Cell cell, Map<SpeciesKey, Double> protectionMap) {
        List<Animal> consumers;
        cell.getLock().lock();
        try {
            // All animals act in randomized order for fairness
            consumers = new ArrayList<>(cell.getAnimals());
        } finally {
            cell.getLock().unlock();
        }
        
        // Sorting by initiative (jittered weight + speed)
        consumers.sort((a, b) -> {
            double initiativeA = (a.getWeight() * 0.7 + a.getSpeed() * 0.3) 
                * (0.8 + ThreadLocalRandom.current().nextDouble() * 0.4);
            double initiativeB = (b.getWeight() * 0.7 + b.getSpeed() * 0.3) 
                * (0.8 + ThreadLocalRandom.current().nextDouble() * 0.4);
            return Double.compare(initiativeB, initiativeA);
        });

        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, island.getTickCount(), protectionMap);

        for (Animal consumer : consumers) {
            if (consumer.isAlive() && !consumer.isHibernating()) {
                tryEat(consumer, cell, preyProvider, protectionMap);
            }
        }
    }

    private void tryEat(Animal consumer, Cell cell, PreyProvider preyProvider, Map<SpeciesKey, Double> protectionMap) {
        int attemptsInTick = 0;
        for (Organism prey : preyProvider.getPreyFor(consumer)) {
            if (consumer == prey || !prey.isAlive()) {
                continue;
            }
            attemptsInTick++;

            double successRate = huntingStrategy.calculateSuccessRate(consumer, prey);
            if (successRate > 0) {
                double totalEffort = huntingStrategy.calculateHuntCost(consumer, prey);
                
                if (attemptsInTick > HUNT_FATIGUE_THRESHOLD) {
                    int extraBlocks = (attemptsInTick - 1) / HUNT_FATIGUE_THRESHOLD;
                    totalEffort *= Math.pow(HUNT_FATIGUE_COST_MULTIPLIER, extraBlocks);
                }

                if (!huntingStrategy.isWorthHunting(consumer, prey, successRate, totalEffort)) {
                    continue; 
                }

                if (!consumer.tryConsumeEnergy(totalEffort)) {
                    island.reportDeath(consumer.getSpeciesKey(), DeathCause.HUNGER);
                    return; 
                }

                // Execution with atomic check-and-consume
                boolean success = false;
                if (ThreadLocalRandom.current().nextDouble() < successRate) {
                    cell.getLock().lock();
                    try {
                        if (prey instanceof Animal a) {
                            if (a.isAlive() && cell.removeAnimal(a)) {
                                a.die();
                                consumer.addEnergy(a.getWeight());
                                preyProvider.markAsEaten(a);
                                island.reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                                success = true;
                            }
                        } else if (prey instanceof Caterpillar c) {
                            if (c.isAlive()) {
                                double foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                                double eaten = c.consumeBiomass(foodNeeded);
                                consumer.addEnergy(eaten);
                                success = true;
                            }
                        }
                    } finally {
                        cell.getLock().unlock();
                    }
                    
                    if (success && consumer.getCurrentEnergy() >= consumer.getFoodForSaturation()) {
                        return; 
                    }
                } 
                
                if (!success && prey instanceof Animal a) {
                    preyProvider.markAsHiding(a);
                    a.tryConsumeEnergy(a.getMaxEnergy() * ESCAPE_ENERGY_COST_PERCENT);
                }
            }
        }

        // --- Plant Feeding Logic ---
        SpeciesKey consumerKey = consumer.getSpeciesKey();
        double foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
        if (foodNeeded <= 0) {
            return;
        }

        int canEatPlants = interactionMatrix.getChance(consumerKey, SpeciesKey.PLANT);
        if (canEatPlants > 0) {
            // 1. Try eating Cabbage first
            Plant cabbage = cell.getPlant(SpeciesKey.CABBAGE);
            if (cabbage != null && cabbage.isAlive()) {
                if (!isPlantProtected(cabbage, protectionMap)) {
                    double eaten = cabbage.consumeBiomass(foodNeeded);
                    consumer.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) {
                        return;
                    }
                }
            }

            // 2. Try eating Grass 
            Plant grass = cell.getPlant(SpeciesKey.GRASS);
            if (grass != null && grass.isAlive()) {
                if (!isPlantProtected(grass, protectionMap)) {
                    double eaten = grass.consumeBiomass(foodNeeded);
                    consumer.addEnergy(eaten);
                }
            }
        }
    }

    private boolean isPlantProtected(Plant plant, Map<SpeciesKey, Double> protectionMap) {
        Double hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && ThreadLocalRandom.current().nextDouble() < hideChance;
    }
}
