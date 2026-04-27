package com.island.service;

import static com.island.config.SimulationConstants.HUNT_FATIGUE_COST_MULTIPLIER;
import static com.island.config.SimulationConstants.HUNT_FATIGUE_THRESHOLD;

import com.island.config.EnergyPolicy;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.DefaultHuntingStrategy;
import com.island.content.HuntingStrategy;
import com.island.content.Organism;
import com.island.content.PreyProvider;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service responsible for feeding logic of all animals.
 */
public class FeedingService extends AbstractService {
    private final InteractionMatrix interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;
    private Map<SpeciesKey, Double> protectionMap;

    public FeedingService(Island island, InteractionMatrix interactionMatrix, 
                          SpeciesRegistry speciesRegistry, ExecutorService executor) {
        super(island, executor);
        this.interactionMatrix = interactionMatrix;
        this.speciesRegistry = speciesRegistry;
        this.huntingStrategy = new DefaultHuntingStrategy(interactionMatrix);
    }

    @Override
    public void run() {
        // Centralized: calculate protection map once per tick
        this.protectionMap = getIsland().getProtectionMap(speciesRegistry);
        super.run();
    }

    @Override
    protected void processCell(Cell cell) {
        List<Animal> consumers;
        cell.getLock().lock();
        try {
            // Take a snapshot to process safely
            consumers = new ArrayList<>(cell.getAnimals());
        } finally {
            cell.getLock().unlock();
        }
        
        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, getIsland().getTickCount(), protectionMap);

        for (Animal consumer : consumers) {
            if (consumer.isAlive() && !consumer.isHibernating()) {
                tryEat(consumer, cell, preyProvider);
            }
        }
    }

    private void tryEat(Animal consumer, Cell cell, PreyProvider preyProvider) {
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
                    getIsland().reportDeath(consumer.getSpeciesKey(), DeathCause.HUNGER);
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
                                getIsland().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                                success = true;
                            }
                        } else if (prey instanceof Biomass b) {
                            if (b.getBiomass() > 0) {
                                double foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                                double eaten = b.consumeBiomass(foodNeeded);
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
                    a.tryConsumeEnergy(a.getMaxEnergy() * EnergyPolicy.ESCAPE_LOSS.getFactor());
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
            Biomass cabbage = cell.getBiomass(SpeciesKey.CABBAGE);
            if (cabbage != null && cabbage.getBiomass() > 0) {
                if (!isPlantProtected(cabbage)) {
                    double eaten = cabbage.consumeBiomass(foodNeeded);
                    consumer.addEnergy(eaten);
                    foodNeeded -= eaten;
                    if (foodNeeded <= 0) {
                        return;
                    }
                }
            }

            // 2. Try eating Grass 
            Biomass grass = cell.getBiomass(SpeciesKey.GRASS);
            if (grass != null && grass.getBiomass() > 0) {
                if (!isPlantProtected(grass)) {
                    double eaten = grass.consumeBiomass(foodNeeded);
                    consumer.addEnergy(eaten);
                }
            }
        }
    }

    private boolean isPlantProtected(Biomass plant) {
        Double hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && ThreadLocalRandom.current().nextDouble() < hideChance;
    }
}
