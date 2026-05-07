package com.island.nature.entities.strategy;

import com.island.nature.config.Configuration;
import com.island.nature.model.Cell;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.island.engine.core.SimulationNode;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionProvider;

/**
 * Provider for prey selection within a node using integer arithmetic.
 */
public class PreyProvider {
    private final Cell node;
    private final Configuration config;
    private final InteractionProvider matrix;
    private final int currentTick;
    private final Map<SpeciesKey, Integer> protectionMap;
    private final boolean isWolfPack;
    private final RandomProvider random;

    public PreyProvider(Cell node, InteractionProvider matrix, 
                        int currentTick, Map<SpeciesKey, Integer> protectionMap, RandomProvider random) {
        this(node, matrix, currentTick, protectionMap, false, random);
    }

    public PreyProvider(Cell node, InteractionProvider matrix, 
                        int currentTick, Map<SpeciesKey, Integer> protectionMap, 
                        boolean isWolfPack, RandomProvider random) {
        this.node = node;
        this.config = node.getConfig();
        this.matrix = matrix;
        this.currentTick = currentTick;
        this.protectionMap = protectionMap;
        this.isWolfPack = isWolfPack;
        this.random = random;
    }

    public List<Organism> getPreyFor(Animal predator) {
        List<Organism> buffet = buildBuffet(predator);
        
        // Strategy: prefer prey that gives more energy relative to its weight/size
        buffet.sort(Comparator.comparingLong(Organism::getWeight).reversed());
        
        return buffet;
    }

    private List<Organism> buildBuffet(Animal predator) {
        List<Organism> potential = new ArrayList<>();
        boolean canHuntAsPack = isWolfPack && predator.getAnimalType().isPackHunter();

        Map<SpeciesKey, Organism> uniquePrey = new HashMap<>();

        // 1. Animals - group by species
        node.forEachAnimal(a -> {
            if (a != predator && a.isAlive() && !uniquePrey.containsKey(a.getSpeciesKey())) {
                int baseChance = matrix.getChance(predator.getSpeciesKey(), a.getSpeciesKey());
                boolean canHunt = baseChance > 0;

                if (!canHunt && canHuntAsPack && config != null && a.getWeight() > 150 * config.getScale1M()) {
                    canHunt = true;
                }

                if (canHunt && !a.isProtected(currentTick)) {
                    uniquePrey.put(a.getSpeciesKey(), a);
                }
            }
        });

        potential.addAll(uniquePrey.values());

        // 2. Plants/Biomass
        node.forEachEntity(e -> {
            if (e instanceof Biomass b && b.getBiomass() > 0 && matrix.getChance(predator.getSpeciesKey(), b.getSpeciesKey()) > 0) {
                if (!isPlantProtected(b)) {
                    potential.add(b);
                }
            }
        });
        
        Collections.shuffle(potential);
        return potential;
    }

    private boolean isPlantProtected(Biomass plant) {
        if (protectionMap == null) {
            return false;
        }
        Integer hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && random.nextInt(0, 100) < hideChance;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
    }

    public void markAsEaten(Organism prey) {
        // Handled by node.removeEntity
    }
}