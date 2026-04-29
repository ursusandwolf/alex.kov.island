package com.island.service;

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
        if (predators.isEmpty()) {
            return;
        }

        List<Animal> packHunters = predators.stream()
                .filter(p -> p.getAnimalType().isPackHunter())
                .toList();
        
        if (packHunters.size() >= minPackSize) {
            processPackHunting(packHunters, cell);
            List<Animal> others = new java.util.ArrayList<>(predators);
            others.removeAll(packHunters);
            predators = others;
        }

        for (Animal predator : predators) {
            if (predator.isAlive() && shouldAct(predator, tickCount)) {
                tryEat(predator, cell);
            }
        }
    }

    private void processHerbivores(Cell cell, int tickCount) {
        // High limit for herbivores (500) to ensure everyone in a standard cell can eat.
        // Skipping meals is the primary cause of starvation in dense populations.
        forEachSampled(cell.getHerbivores(), 500, herbivore -> {
            if (herbivore.isAlive() && shouldAct(herbivore, tickCount)) {
                tryEat(herbivore, cell);
            }
        });
    }

    private boolean shouldAct(Animal animal, int tickCount) {
        if (!animal.canPerformAction()) {
            return false;
        }
        if (tickCount == 0) {
            return true;
        }
        return !animal.getAnimalType().isColdBlooded() || (tickCount % 3 == 0);
    }

    private void processPackHunting(List<Animal> pack, Cell cell) {
        PreyProvider packPreyProvider = new PreyProvider(cell, interactionMatrix, 0, protectionMap, true, getRandom());
        int maxKills = Math.max(1, pack.size() / 2);
        int kills = 0;

        for (int i = 0; i < pack.size() && kills < maxKills; i++) {
            Organism prey = huntingStrategy.selectPrey(pack.get(0), packPreyProvider);
            if (prey instanceof Animal a) {
                if (a.isAlive() && !a.isProtected(0) && cell.removeAnimal(a)) {
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
                if (a.isAlive() && !a.isProtected(0)) {
                    double chance = interactionMatrix.getChance(consumer.getSpeciesKey(), a.getSpeciesKey());
                    int preyCount = cell.getOrganismCount(a.getSpeciesKey());
                    if (preyCount > a.getAnimalType().getMaxPerCell() / 2) {
                        chance += 15; 
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
            } else if (prey instanceof Biomass b && b.getBiomass() > 0 && !isPlantProtected(b)) {
                double foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                consumer.addEnergy(b.consumeBiomass(foodNeeded));
                success = true;
            }
        }
        
        if (!success && consumer.getAnimalType().isPredator()) {
            consumer.consumeEnergy(consumer.getMaxEnergy() * 0.05);
        } else if (!success) {
            consumer.consumeEnergy(consumer.getMaxEnergy() * 0.03);
        }
    }

    private boolean isPlantProtected(Biomass plant) {
        Double hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && getRandom().nextDouble() < hideChance;
    }
}
