package com.island.service;

import static com.island.config.SimulationConstants.HUNT_FATIGUE_COST_MULTIPLIER;
import static com.island.config.SimulationConstants.HUNT_FATIGUE_THRESHOLD;

import com.island.config.EnergyPolicy;
import com.island.content.Animal;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.HuntingStrategy;
import com.island.content.Organism;
import com.island.content.PreyProvider;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for feeding logic of all animals.
 */
public class FeedingService extends AbstractService {
    private final InteractionMatrix interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;
    private final int minPackSize;
    private Map<SpeciesKey, Double> protectionMap;

    public FeedingService(Island island, InteractionMatrix interactionMatrix, 
                          SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy, 
                          ExecutorService executor, RandomProvider random) {
        this(island, interactionMatrix, speciesRegistry, huntingStrategy, executor, 
                com.island.config.SimulationConstants.WOLF_PACK_MIN_SIZE, random);
    }

    public FeedingService(Island island, InteractionMatrix interactionMatrix, 
                          SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy, 
                          ExecutorService executor, int minPackSize, RandomProvider random) {
        super(island, executor, random);
        this.interactionMatrix = interactionMatrix;
        this.speciesRegistry = speciesRegistry;
        this.huntingStrategy = huntingStrategy;
        this.minPackSize = minPackSize;
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
        
        // --- Pack Hunting Logic (Wolf) ---
        List<Animal> wolves = new ArrayList<>();
        for (Animal a : consumers) {
            if (a.getSpeciesKey().equals(SpeciesKey.WOLF) && a.isAlive() && !a.isHibernating()) {
                wolves.add(a);
            }
        }

        if (wolves.size() >= minPackSize) {
            processWolfPack(wolves, cell);
            // Remove wolves from individual processing
            consumers.removeAll(wolves);
        }

        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, getIsland().getTickCount(), protectionMap, getRandom());

        for (Animal consumer : consumers) {
            if (consumer.isAlive() && !consumer.isHibernating()) {
                tryEat(consumer, cell, preyProvider);
            }
        }
    }

    private void processWolfPack(List<Animal> pack, Cell cell) {
        // Use a specialist prey provider for the pack (sees Bears)
        PreyProvider packPreyProvider = new PreyProvider(cell, interactionMatrix, 
                                            getIsland().getTickCount(), protectionMap, true, getRandom());
        
        // Pick one lead wolf to find prey for the whole pack
        Animal leadWolf = pack.get(0);
        
        for (Organism prey : packPreyProvider.getPreyFor(leadWolf)) {
            if (!prey.isAlive()) {
                continue;
            }

            int baseChance = interactionMatrix.getChance(SpeciesKey.WOLF, prey.getSpeciesKey());
            // Special chance for Bear if not in matrix
            if (baseChance == 0 && prey.getSpeciesKey().equals(SpeciesKey.BEAR)) {
                baseChance = Math.min(com.island.config.SimulationConstants.WOLF_PACK_BEAR_HUNT_MAX_CHANCE, pack.size());
            }

            if (baseChance == 0) {
                continue;
            }

            double successRate = huntingStrategy.calculatePackSuccessRate(pack, prey, baseChance);
            double individualEffort = huntingStrategy.calculateHuntCost(leadWolf, prey) / pack.size();
            
            // ROI check for the pack
            if (!huntingStrategy.isWorthHunting(leadWolf, prey, successRate, individualEffort * pack.size())) {
                continue;
            }

            // Everyone spends energy to try hunt
            for (Animal wolf : pack) {
                if (!wolf.tryConsumeEnergy(individualEffort)) {
                    getIsland().reportDeath(wolf.getSpeciesKey(), DeathCause.HUNGER);
                }
            }

            if (getRandom().nextDouble() < successRate) {
                cell.getLock().lock();
                try {
                    boolean success = false;
                    if (prey instanceof Animal a) {
                        if (a.isAlive() && cell.removeAnimal(a)) {
                            a.die();
                            double gainPerWolf = a.getWeight() / pack.size();
                            for (Animal wolf : pack) {
                                if (wolf.isAlive()) {
                                    wolf.addEnergy(gainPerWolf);
                                }
                            }
                            packPreyProvider.markAsEaten(a);
                            getIsland().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                            success = true;
                        }
                    } else if (prey instanceof Biomass b) {
                        if (b.getBiomass() > 0) {
                            double totalNeeded = 0;
                            for (Animal wolf : pack) {
                                totalNeeded += (wolf.getFoodForSaturation() - wolf.getCurrentEnergy());
                            }
                            double eaten = b.consumeBiomass(totalNeeded);
                            double gainPerWolf = eaten / pack.size();
                            for (Animal wolf : pack) {
                                if (wolf.isAlive()) {
                                    wolf.addEnergy(gainPerWolf);
                                }
                            }
                            success = true;
                        }
                    }

                    if (success) {
                        return;
                    } // Pack is satisfied for this tick
                } finally {
                    cell.getLock().unlock();
                }
            } else if (prey instanceof Animal a) {
                packPreyProvider.markAsHiding(a);
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
                if (getRandom().nextDouble() < successRate) {
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
        return hideChance != null && getRandom().nextDouble() < hideChance;
    }
}
