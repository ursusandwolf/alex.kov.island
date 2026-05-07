package com.island.nature.service;

import com.island.engine.ecs.Component;
import com.island.nature.entities.components.ConsumableComponent;
import com.island.nature.entities.components.HealthComponent;
import com.island.nature.entities.components.MetabolismComponent;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.domain.TaskRegistry;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.HuntingStrategy;
import com.island.nature.entities.strategy.PreyProvider;
import com.island.nature.model.Cell;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * ECS System responsible for animal feeding logic.
 * Replaces FeedingService.
 */
public class AnimalFeedingSystem extends NatureEntitySystem {
    private final AnimalFactory animalFactory;
    private final InteractionProvider interactionMatrix;
    private final SpeciesRegistry speciesRegistry;
    private final HuntingStrategy huntingStrategy;

    public AnimalFeedingSystem(NatureWorld world, AnimalFactory animalFactory,
                               InteractionProvider interactionMatrix,
                               SpeciesRegistry speciesRegistry, HuntingStrategy huntingStrategy,
                               ExecutorService executor, RandomProvider random) {
        super(world, executor, random);
        this.animalFactory = animalFactory;
        this.interactionMatrix = interactionMatrix;
        this.speciesRegistry = speciesRegistry;
        this.huntingStrategy = huntingStrategy;
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of();
    }

    @Override
    public List<Class<? extends Component>> writeComponents() {
        return List.of(HealthComponent.class, MetabolismComponent.class);
    }

    @Override
    public int priority() {
        return TaskRegistry.PRIORITY_FEEDING;
    }

    @Override
    protected void doProcessCell(Cell cell, int tickCount) {
        // We override doProcessCell to handle pack hunting logic which requires group processing
        processPredators(cell, tickCount);
        processHerbivores(cell, tickCount);
    }

    @Override
    protected void process(Organism entity, Cell cell, int tickCount) {
        // This is called by NatureEntitySystem.doProcessCell if we didn't override it.
        // But since we did, we can use it for individual feeding if needed.
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
        if (packHunters.size() >= config.getWolfPackMinSize()) {
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
        node.forEachHerbivoreSampled(config.getFeedingLodLimit(), getRandom(), herbivore -> {
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
            if (preyCandidate != null) {
                ConsumableComponent consumable = preyCandidate.getComponent(ConsumableComponent.class);
                if (consumable != null && consumable.isAnimal()) {
                    // Safe cast as it's an animal consumable
                    Animal actualPrey = findActualPrey(node, preyCandidate.getSpeciesKey(), pack.get(0));
                    if (actualPrey != null && actualPrey.isAlive() && !isProtected(actualPrey)) {
                        int baseChance = interactionMatrix.getChance(pack.get(0).getSpeciesKey(), actualPrey.getSpeciesKey());
                        int packChanceBP = huntingStrategy.calculatePackSuccessRate(pack, actualPrey, baseChance);
                        
                        if (getRandom().nextInt(0, config.getScale10K()) < packChanceBP) {
                            long gain = consumable.consume(actualPrey.getWeight());
                            if (node.removeEntity(actualPrey)) {
                                long gainPerWolf = gain / pack.size();
                                for (Animal wolf : pack) {
                                    if (wolf.isAlive()) {
                                        wolf.addEnergy(gainPerWolf);
                                    }
                                }
                                packPreyProvider.markAsEaten(actualPrey);
                                animalFactory.releaseAnimal(actualPrey);
                                kills++;
                            }
                        } else {
                            long strikeCost = huntingStrategy.calculateHuntCost(pack.get(0), actualPrey);
                            long penalty = (strikeCost * config.getPredatorFailHuntPenaltyBP()) / config.getScale10K();
                            for (Animal wolf : pack) {
                                wolf.consumeEnergy(penalty);
                            }
                        }
                    }
                }
            } else {
                break; 
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
            
            ConsumableComponent consumable = preyCandidate.getComponent(ConsumableComponent.class);
            if (consumable == null) {
                continue;
            }

            strikeAttempted = true;
            if (consumable.isAnimal()) {
                Animal actualPrey = findActualPrey(node, preyCandidate.getSpeciesKey(), consumer);
                if (actualPrey != null && actualPrey.isAlive() && !isProtected(actualPrey)) {
                    int chance = interactionMatrix.getChance(consumer.getSpeciesKey(), actualPrey.getSpeciesKey());
                    int preyCount = node.getOrganismCount(actualPrey.getSpeciesKey());
                    if (preyCount > actualPrey.getAnimalType().getMaxPerCell() / 2) {
                        chance += config.getOverpopulationHuntBonusPercent(); 
                    }

                    if (getRandom().nextInt(0, 100) < chance) {
                        long gain = consumable.consume(actualPrey.getWeight());
                        if (node.removeEntity(actualPrey)) {
                            consumer.addEnergy(gain);
                            preyProvider.markAsEaten(actualPrey);
                            animalFactory.releaseAnimal(actualPrey);
                            success = true;
                        }
                    }
                }
            } else {
                // Biomass consumption
                if (!isPlantProtected(preyCandidate.getSpeciesKey())) {
                    long foodNeeded = consumer.getFoodForSaturation() - consumer.getCurrentEnergy();
                    // We still use direct call for biomass for now as consumeAction needs Cell context
                    // Or we can enhance ConsumableComponent to take context or handle it here
                    if (preyCandidate instanceof com.island.nature.entities.core.Biomass b) {
                        consumer.addEnergy(b.consumeBiomass(foodNeeded, node));
                        success = true;
                    }
                }
            }
        }
        
        if (!success && strikeAttempted) {
            long penaltyPercent = consumer.getAnimalType().isPredator() ? config.getPredatorFailHuntPenaltyBP() : config.getHerbivoreFailFeedPenaltyBP();
            consumer.consumeEnergy((consumer.getMaxEnergy() * penaltyPercent) / config.getScale10K());
        }
    }

    private Animal findActualPrey(Cell node, SpeciesKey speciesKey, Animal consumer) {
        AnimalType type = speciesRegistry.getAnimalType(speciesKey).orElse(null);
        if (type == null) {
            return null;
        }
        
        for (int i = 0; i < 3; i++) {
            Animal candidate = node.getRandomAnimalByType(type, getRandom());
            if (candidate != null && candidate != consumer) {
                return candidate;
            }
            if (candidate == null) {
                break;
            }
        }
        return null;
    }
}
