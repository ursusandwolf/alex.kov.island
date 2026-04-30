package com.island.service;

import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
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
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.island.config.SimulationConstants.FEEDING_LOD_LIMIT;
import static com.island.config.SimulationConstants.HERBIVORE_FAIL_FEED_PENALTY_BP;
import static com.island.config.SimulationConstants.OVERPOPULATION_HUNT_BONUS_PERCENT;
import static com.island.config.SimulationConstants.PREDATOR_FAIL_HUNT_PENALTY_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

/**
 * Service responsible for feeding logic using integer-based arithmetic.
 */
public class FeedingService extends AbstractService<Cell> {
    private final AnimalFactory animalFactory;
    private final InteractionProvider interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;
    private final int minPackSize;

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
    protected void processCell(Cell cell, int tickCount) {
        processPredators(cell, tickCount);
        processHerbivores(cell, tickCount);
    }

    private void processPredators(Cell cell, int tickCount) {
        List<Animal> packHunters = new java.util.ArrayList<>();
        List<Animal> others = new java.util.ArrayList<>();

        cell.forEachPredator(p -> {
            if (p.getAnimalType().isPackHunter()) {
                packHunters.add(p);
            } else {
                others.add(p);
            }
        });

        if (packHunters.size() >= minPackSize) {
            processPackHunting(packHunters, cell);
        } else {
            others.addAll(packHunters);
        }

        for (Animal predator : others) {
            if (predator.isAlive() && shouldAct(predator, AnimalType.Action.FEED, tickCount)) {
                tryEat(predator, cell);
            }
        }
    }

    private void processHerbivores(Cell cell, int tickCount) {
        cell.forEachHerbivoreSampled(FEEDING_LOD_LIMIT, getRandom(), herbivore -> {
            if (herbivore.isAlive() && shouldAct(herbivore, AnimalType.Action.FEED, tickCount)) {
                tryEat(herbivore, cell);
            }
        });
    }

    private void processPackHunting(List<Animal> pack, Cell cell) {
        PreyProvider packPreyProvider = new PreyProvider(cell, interactionMatrix, 0, protectionMap, true, getRandom());
        int maxKills = Math.max(1, pack.size() / 2);
        int kills = 0;

        for (int i = 0; i < pack.size() && kills < maxKills; i++) {
            Organism prey = huntingStrategy.selectPrey(pack.get(0), packPreyProvider);
            if (prey instanceof Animal a) {
                if (a.isAlive() && !isProtected(a) && cell.removeAnimal(a)) {
                    a.die();
                    long gainPerWolf = a.getWeight() / pack.size();
                    for (Animal wolf : pack) {
                        if (wolf.isAlive()) {
                            wolf.addEnergy(gainPerWolf);
                        }
                    }
                    packPreyProvider.markAsEaten(a);
                    getWorld().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                    animalFactory.releaseAnimal(a);
                    kills++;
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
        int maxAttempts = consumer.getAnimalType().isPredator() ? 5 : 3;

        while (!success && attempts < maxAttempts) {
            attempts++;
            Organism prey = huntingStrategy.selectPrey(consumer, preyProvider);
            if (prey == null) {
                break;
            }

            if (prey instanceof Animal a) {
                if (a.isAlive() && !isProtected(a)) {
                    int chance = interactionMatrix.getChance(consumer.getSpeciesKey(), a.getSpeciesKey());
                    int preyCount = cell.getOrganismCount(a.getSpeciesKey());
                    if (preyCount > a.getAnimalType().getMaxPerCell() / 2) {
                        chance += OVERPOPULATION_HUNT_BONUS_PERCENT; 
                    }

                    if (getRandom().nextInt(0, 100) < chance && cell.removeAnimal(a)) {
                        a.die();
                        consumer.addEnergy(a.getWeight());
                        preyProvider.markAsEaten(a);
                        getWorld().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                        animalFactory.releaseAnimal(a);
                        success = true;
                    }
                }
            } else if (prey instanceof Biomass b && b.getBiomass() > 0 && !isPlantProtected(b.getSpeciesKey())) {
                long foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                consumer.addEnergy(b.consumeBiomass(foodNeeded, cell));
                success = true;
            }
        }
        
        if (!success && consumer.getAnimalType().isPredator()) {
            consumer.consumeEnergy((consumer.getMaxEnergy() * PREDATOR_FAIL_HUNT_PENALTY_BP) / SCALE_10K);
        } else if (!success) {
            consumer.consumeEnergy((consumer.getMaxEnergy() * HERBIVORE_FAIL_FEED_PENALTY_BP) / SCALE_10K);
        }
    }
}
