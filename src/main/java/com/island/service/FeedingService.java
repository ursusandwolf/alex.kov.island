package com.island.service;

import static com.island.config.SimulationConstants.FEEDING_LOD_LIMIT;
import static com.island.config.SimulationConstants.HERBIVORE_FAIL_FEED_PENALTY_BP;
import static com.island.config.SimulationConstants.OVERPOPULATION_HUNT_BONUS_PERCENT;
import static com.island.config.SimulationConstants.PREDATOR_FAIL_HUNT_PENALTY_BP;
import static com.island.config.SimulationConstants.SCALE_10K;

import com.island.config.SimulationConstants;
import com.island.content.Animal;
import com.island.content.AnimalFactory;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.HuntingStrategy;
import com.island.content.NatureWorld;
import com.island.content.Organism;
import com.island.content.PreyProvider;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesRegistry;
import com.island.engine.SimulationNode;
import com.island.model.Cell;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Service responsible for feeding logic using integer-based arithmetic.
 */
public class FeedingService extends AbstractService {
    private final AnimalFactory animalFactory;
    private final InteractionProvider interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;
    private final int minPackSize;

    public FeedingService(NatureWorld world, AnimalFactory animalFactory, 
                          InteractionProvider interactionMatrix, 
                          SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy, 
                          ExecutorService executor, RandomProvider random) {
        this(world, animalFactory, interactionMatrix, speciesRegistry, huntingStrategy, executor, 
                SimulationConstants.WOLF_PACK_MIN_SIZE, random);
    }

    public FeedingService(NatureWorld world, AnimalFactory animalFactory, 
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
    public void processCell(Cell cell, int tickCount) {
        processPredators(cell, tickCount);
        processHerbivores(cell, tickCount);
    }

    private void processPredators(Cell node, int tickCount) {
        List<Animal> packHunters = new ArrayList<>();
        List<Animal> soloHunters = new ArrayList<>();

        node.forEachPredator(p -> {
            if (p.getAnimalType().isPackHunter()) {
                packHunters.add(p);
            } else {
                if (p.isAlive() && shouldAct(p, AnimalType.Action.FEED, tickCount)) {
                    soloHunters.add(p);
                }
            }
        });

        // Process solo hunters
        for (Animal predator : soloHunters) {
            if (predator.isAlive()) {
                tryEat(predator, node);
            }
        }

        // Process pack hunters
        if (packHunters.size() >= minPackSize) {
            processPackHunting(packHunters, node);
        } else {
            for (Animal wolf : packHunters) {
                if (wolf.isAlive() && shouldAct(wolf, AnimalType.Action.FEED, tickCount)) {
                    tryEat(wolf, node);
                }
            }
        }
    }

    private void processHerbivores(Cell node, int tickCount) {
        node.forEachHerbivoreSampled(FEEDING_LOD_LIMIT, getRandom(), herbivore -> {
            if (herbivore.isAlive() && shouldAct(herbivore, AnimalType.Action.FEED, tickCount)) {
                tryEat(herbivore, node);
            }
        });
    }

    private void processPackHunting(List<Animal> pack, Cell node) {
        if (pack.isEmpty()) {
            return;
        }
        
        PreyProvider packPreyProvider = new PreyProvider(node, interactionMatrix, 0, protectionMap, true, getRandom());
        int maxKills = Math.max(1, pack.size() / 2);
        int kills = 0;
        int attempts = 0;
        int maxAttempts = 5;

        while (kills < maxKills && attempts < maxAttempts) {
            attempts++;
            Organism preyCandidate = huntingStrategy.selectPackPrey(pack, packPreyProvider);
            if (preyCandidate instanceof Animal aCandidate) {
                Animal a = findActualPrey(node, aCandidate.getSpeciesKey(), pack.get(0));
                if (a != null && a.isAlive() && !isProtected(a)) {
                    int baseChance = interactionMatrix.getChance(pack.get(0).getSpeciesKey(), a.getSpeciesKey());
                    int packChanceBP = huntingStrategy.calculatePackSuccessRate(pack, a, baseChance);
                    
                    int roll = getRandom().nextInt(0, SCALE_10K);
                    if (roll < packChanceBP) {
                        if (node.removeEntity(a)) {
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
                    } else {
                        // Pack hunt failure - all participants lose energy
                        long strikeCost = huntingStrategy.calculateHuntCost(pack.get(0), a);
                        long penalty = (strikeCost * PREDATOR_FAIL_HUNT_PENALTY_BP) / SCALE_10K;
                        for (Animal wolf : pack) {
                            wolf.consumeEnergy(penalty);
                        }
                    }
                }
            } else {
                break; // No suitable prey found
            }
        }
    }

    private void tryEat(Animal consumer, Cell node) {
        if (consumer.getCurrentEnergy() >= consumer.getFoodForSaturation()) {
            return;
        }

        PreyProvider preyProvider = new PreyProvider(node, interactionMatrix, 0, protectionMap, getRandom());
        int attempts = 0;
        boolean success = false;
        boolean strikeAttempted = false;
        int maxAttempts = consumer.getAnimalType().isPredator() ? 5 : 3;

        while (!success && attempts < maxAttempts) {
            attempts++;
            Organism preyCandidate = huntingStrategy.selectPrey(consumer, preyProvider);
            if (preyCandidate == null) {
                break;
            }
            
            strikeAttempted = true;
            if (preyCandidate instanceof Animal aCandidate) {
                // Pick a random alive animal of this species from the cell to avoid collision with other predators
                Animal a = findActualPrey(node, aCandidate.getSpeciesKey(), consumer);
                if (a != null && a.isAlive() && !isProtected(a)) {
                    int chance = interactionMatrix.getChance(consumer.getSpeciesKey(), a.getSpeciesKey());
                    int preyCount = node.getOrganismCount(a.getSpeciesKey());
                    if (preyCount > a.getAnimalType().getMaxPerCell() / 2) {
                        chance += OVERPOPULATION_HUNT_BONUS_PERCENT; 
                    }

                    if (getRandom().nextInt(0, 100) < chance && node.removeEntity(a)) {
                        a.die();
                        consumer.addEnergy(a.getWeight());
                        preyProvider.markAsEaten(a);
                        getWorld().reportDeath(a.getSpeciesKey(), DeathCause.EATEN);
                        animalFactory.releaseAnimal(a);
                        success = true;
                    }
                }
            } else if (preyCandidate instanceof Biomass b && b.getBiomass() > 0 && !isPlantProtected(b.getSpeciesKey())) {
                long foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                consumer.addEnergy(b.consumeBiomass(foodNeeded, node));
                success = true;
            }
        }
        
        if (!success && strikeAttempted) {
            if (consumer.getAnimalType().isPredator()) {
                consumer.consumeEnergy((consumer.getMaxEnergy() * PREDATOR_FAIL_HUNT_PENALTY_BP) / SCALE_10K);
            } else {
                consumer.consumeEnergy((consumer.getMaxEnergy() * HERBIVORE_FAIL_FEED_PENALTY_BP) / SCALE_10K);
            }
        }
    }

    private Animal findActualPrey(Cell node, SpeciesKey speciesKey, Animal consumer) {
        AnimalType type = speciesRegistry.getAnimalType(speciesKey).orElse(null);
        if (type == null) {
            return null;
        }
        Animal candidate = node.getRandomAnimalByType(type, getRandom());
        if (candidate == consumer) {
            if (node.getOrganismCount(speciesKey) <= 1) {
                return null;
            }
            return null;
        }
        return candidate;
    }
}
