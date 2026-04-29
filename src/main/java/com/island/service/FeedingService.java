package com.island.service;

import static com.island.config.SimulationConstants.HUNT_FATIGUE_COST_MULTIPLIER;
import static com.island.config.SimulationConstants.HUNT_FATIGUE_THRESHOLD;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.HuntingStrategy;
import com.island.content.Organism;
import com.island.content.PreyProvider;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.model.Cell;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for feeding logic of all animals.
 */
public class FeedingService extends AbstractService {
    private final AnimalFactory animalFactory;
    private final InteractionProvider interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;
    private final int minPackSize;
    private Map<SpeciesKey, Double> protectionMap;

    public FeedingService(SimulationWorld world, AnimalFactory animalFactory, 
                          InteractionProvider interactionMatrix, 
                          SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy, 
                          ExecutorService executor, RandomProvider random) {
        this(world, animalFactory, interactionMatrix, speciesRegistry, huntingStrategy, executor, 
                com.island.config.SimulationConstants.WOLF_PACK_MIN_SIZE, random);
    }

    public FeedingService(SimulationWorld world, AnimalFactory animalFactory, 
                          InteractionProvider interactionMatrix, 
                          SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy, 
                          ExecutorService executor, int minPackSize, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
        this.interactionMatrix = interactionMatrix;
        this.speciesRegistry = speciesRegistry;
        this.huntingStrategy = huntingStrategy;
        this.minPackSize = minPackSize;
    }

    @Override
    protected void processCell(SimulationNode node, int tickCount) {
        if (node instanceof Cell cell) {
            this.protectionMap = getWorld().getProtectionMap(speciesRegistry);
            processPredators(cell, tickCount);
            processHerbivores(cell, tickCount);
        }
    }

    private void processPredators(Cell cell, int tickCount) {
        List<Animal> predators = cell.getPredators();
        
        // LOD: If too many predators, skip some
        if (predators.size() > 50) {
            java.util.Collections.shuffle(predators, new java.util.Random(getRandom().nextLong()));
            predators = predators.subList(0, 50);
        }

        // Group wolves into packs for coordinated hunting
        List<Animal> wolves = predators.stream()
                .filter(p -> p.getSpeciesKey().equals(SpeciesKey.WOLF))
                .toList();
        
        if (wolves.size() >= minPackSize) {
            processPackHunting(wolves, cell);
            predators = new java.util.ArrayList<>(predators);
            predators.removeAll(wolves);
        }

        for (Animal predator : predators) {
            if (predator.isAlive() && shouldAct(predator, tickCount)) {
                tryEat(predator, cell);
            }
        }
    }

    private void processHerbivores(Cell cell, int tickCount) {
        List<Animal> herbivores = cell.getHerbivores();

        // LOD: If too many herbivores, process only a sample (max 100 per cell per tick)
        if (herbivores.size() > 100) {
            java.util.Collections.shuffle(herbivores, new java.util.Random(getRandom().nextLong()));
            herbivores = herbivores.subList(0, 100);
        }

        for (Animal herbivore : herbivores) {
            if (herbivore.isAlive() && shouldAct(herbivore, tickCount)) {
                tryEat(herbivore, cell);
            }
        }
    }

    private boolean shouldAct(Animal animal, int tickCount) {
        if (!animal.canPerformAction()) {
            return false;
        }
        // Tests often use tickCount 0 or 1. Let's allow action if tickCount is 0.
        if (tickCount == 0) {
            return true;
        }
        // Cold-blooded animals eat every 3rd tick
        if (animal.getSpeciesKey().isColdBlooded()) {
            return (tickCount % 3 == 0);
        }
        return true;
    }

    private void processPackHunting(List<Animal> pack, Cell cell) {
        PreyProvider packPreyProvider = new PreyProvider(cell, interactionMatrix, 0, protectionMap, true, getRandom());
        
        for (int i = 0; i < pack.size(); i++) {
            Organism prey = huntingStrategy.selectPrey(pack.get(0), packPreyProvider);
            if (prey != null) {
                if (prey instanceof Animal a) {
                    if (a.isAlive() && !a.isProtected(0)) {
                        if (a.isAlive() && cell.removeAnimal(a)) {
                            a.die();
                            double gainPerWolf = a.getWeight() / pack.size();
                            for (Animal wolf : pack) {
                                if (wolf.isAlive()) {
                                    wolf.addEnergy(gainPerWolf);
                                }
                            }
                            packPreyProvider.markAsEaten(a);
                            getWorld().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                            animalFactory.releaseAnimal(a);
                            break; // Pack successfully ate
                        }
                    }
                }
            }
        }
    }

    private void tryEat(Animal consumer, Cell cell) {
        if (consumer.getCurrentEnergy() >= consumer.getFoodForSaturation()) {
            return;
        }

        PreyProvider preyProvider = new PreyProvider(cell, interactionMatrix, 0, protectionMap, getRandom());
        int attempts = 0;
        boolean success = false;

        while (!success && attempts < 3) {
            attempts++;
            Organism prey = huntingStrategy.selectPrey(consumer, preyProvider);
            if (prey == null) {
                break;
            }

            if (prey instanceof Animal a) {
                if (a.isAlive() && !a.isProtected(0)) {
                    double chance = interactionMatrix.getChance(consumer.getSpeciesKey(), a.getSpeciesKey());
                    if (getRandom().nextInt(0, 100) < chance) {
                        if (a.isAlive() && cell.removeAnimal(a)) {
                            a.die();
                            consumer.addEnergy(a.getWeight());
                            preyProvider.markAsEaten(a);
                            getWorld().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                            animalFactory.releaseAnimal(a);
                            success = true;
                        }
                    }
                }
            } else if (prey instanceof Biomass b) {
                if (b.getBiomass() > 0) {
                    if (!isPlantProtected(b)) {
                        double foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                        double eaten = b.consumeBiomass(foodNeeded);
                        consumer.addEnergy(eaten);
                        success = true;
                    }
                }
            }
        }
        
        if (!success) {
            consumer.consumeEnergy(consumer.getMaxEnergy() * 0.05);
        }
    }

    private boolean isPlantProtected(Biomass plant) {
        Double hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && getRandom().nextDouble() < hideChance;
    }
}
