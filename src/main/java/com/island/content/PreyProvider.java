package com.island.content;

import com.island.engine.Mortal;
import com.island.engine.SimulationNode;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provider for prey selection within a node using integer arithmetic.
 */
public class PreyProvider {
    private final SimulationNode node;
    private final InteractionProvider matrix;
    private final int currentTick;
    private final Map<SpeciesKey, Integer> protectionMap; // Chance in percent
    private final RandomProvider random;
    private final boolean isWolfPack;

    private List<Organism> buffet;
    private boolean isEatenListChanged = false;

    public PreyProvider(SimulationNode node, InteractionProvider matrix, RandomProvider random) {
        this(node, matrix, 0, Collections.emptyMap(), random);
    }

    public PreyProvider(SimulationNode node, InteractionProvider matrix, int currentTick, 
                        Map<SpeciesKey, Integer> protectionMap, RandomProvider random) {
        this(node, matrix, currentTick, protectionMap, false, random);
    }

    public PreyProvider(SimulationNode node, InteractionProvider matrix, int tick, 
                        Map<SpeciesKey, Integer> protectionMap, boolean isWolfPack, RandomProvider random) {
        this.node = node;
        this.matrix = matrix;
        this.currentTick = tick;
        this.protectionMap = (protectionMap != null) ? protectionMap : Collections.emptyMap();
        this.isWolfPack = isWolfPack;
        this.random = random;
    }

    public List<Organism> getPreyFor(Animal predator) {
        if (buffet == null || isEatenListChanged) {
            buffet = buildBuffet(predator);
            isEatenListChanged = false;
        }
        return buffet;
    }

    private List<Organism> buildBuffet(Animal predator) {
        List<Organism> potential = new ArrayList<>();
        boolean canHuntAsPack = isWolfPack && predator.getAnimalType().isPackHunter();
        
        // Use species grouping from the node's world registry/statistics or iteration
        // For simplicity and speed, we'll iterate once and collect unique species representatives
        Map<SpeciesKey, Organism> uniquePrey = new HashMap<>();

        // 1. Animals - group by species
        node.forEachAnimal(a -> {
            if (a != predator && a.isAlive() && !uniquePrey.containsKey(a.getSpeciesKey())) {
                int baseChance = matrix.getChance(predator.getSpeciesKey(), a.getSpeciesKey());
                boolean canHunt = baseChance > 0;
                
                if (!canHunt && canHuntAsPack && a.getWeight() > 150 * com.island.config.SimulationConstants.SCALE_1M) {
                    canHunt = true;
                }

                if (canHunt && !a.isProtected(currentTick)) {
                    uniquePrey.put(a.getSpeciesKey(), a);
                }
            }
        });
        
        potential.addAll(uniquePrey.values());

        // 2. Plants/Biomass
        node.forEachBiomass(b -> {
            if (b.getBiomass() > 0 && matrix.getChance(predator.getSpeciesKey(), b.getSpeciesKey()) > 0) {
                if (!isPlantProtected(b)) {
                    potential.add(b);
                }
            }
        });

        // Sort by ROI (weight * probability) descending
        potential.sort(Comparator.comparingLong((Organism o) -> {
            long weight = o instanceof Biomass ? ((Biomass) o).getBiomass() : o.getWeight();
            int chance = matrix.getChance(predator.getSpeciesKey(), o.getSpeciesKey());
            return weight * (long) chance;
        }).reversed());

        return potential;
    }

    private boolean isPlantProtected(Biomass plant) {
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
