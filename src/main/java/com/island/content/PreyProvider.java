package com.island.content;

import com.island.content.animals.herbivores.Caterpillar;
import com.island.model.Cell;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Provider for prey selection within a cell.
 * Optimized with caching to avoid repeated calculations during a single predator's turn.
 */
public class PreyProvider {
    private final Cell cell;
    private final InteractionProvider matrix;
    private final int currentTick;
    private final Map<SpeciesKey, Double> protectionMap;
    private final RandomProvider random;
    private final boolean isWolfPack;

    private List<Organism> buffet;
    private boolean isEatenListChanged = false;

    public PreyProvider(Cell cell, InteractionProvider matrix, RandomProvider random) {
        this(cell, matrix, 0, Collections.emptyMap(), random);
    }

    public PreyProvider(Cell cell, InteractionProvider matrix, int currentTick, 
                        Map<SpeciesKey, Double> protectionMap, RandomProvider random) {
        this(cell, matrix, currentTick, protectionMap, false, random);
    }

    public PreyProvider(Cell cell, InteractionProvider matrix, int tick, 
                        Map<SpeciesKey, Double> protectionMap, boolean isWolfPack, RandomProvider random) {
        this.cell = cell;
        this.matrix = matrix;
        this.currentTick = tick;
        this.protectionMap = protectionMap;
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
        
        // 1. Animals
        cell.forEachAnimal(a -> {
            if (a != predator && a.isAlive() && matrix.getChance(predator.getSpeciesKey(), a.getSpeciesKey()) > 0) {
                if (!a.isProtected(currentTick)) {
                    potential.add(a);
                }
            }
        });
        
        // 2. Plants/Biomass
        for (Biomass b : cell.getBiomassContainers()) {
            if (b.getBiomass() > 0 && matrix.getChance(predator.getSpeciesKey(), b.getSpeciesKey()) > 0) {
                if (!isPlantProtected(b)) {
                    potential.add(b);
                }
            }
        }

        // Sort by ROI (weight * probability) descending
        potential.sort(Comparator.comparingDouble((Organism o) -> {
            double weight = o instanceof Biomass ? ((Biomass) o).getBiomass() : o.getWeight();
            int chance = matrix.getChance(predator.getSpeciesKey(), o.getSpeciesKey());
            return weight * chance;
        }).reversed());

        return potential;
    }

    private boolean isPlantProtected(Biomass plant) {
        Double hideChance = protectionMap.get(plant.getSpeciesKey());
        return hideChance != null && random.nextDouble() < hideChance;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
    }

    public void markAsEaten(Organism prey) {
        // Handled by cell.removeAnimal under lock in FeedingService
    }
}
