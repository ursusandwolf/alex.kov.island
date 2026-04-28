package com.island.content;

import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.Biomass;
import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import com.island.util.RandomUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Optimized food provider that simulates a mixed "buffet" with competition.
 * Refactored to use bucketed prey sources (O(1) access) instead of linear scans and shuffles.
 */
public class PreyProvider {
    private final Cell cell;
    private final InteractionMatrix matrix;
    private final int currentTick;
    private final Map<SpeciesKey, Double> protectionMap;

    private final boolean isWolfPack;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int tick, Map<SpeciesKey, Double> protectionMap) {
        this(cell, matrix, tick, protectionMap, false);
    }

    public PreyProvider(Cell cell, InteractionMatrix matrix, int tick, 
                        Map<SpeciesKey, Double> protectionMap, boolean isWolfPack) {
        this.cell = cell;
        this.matrix = matrix;
        this.currentTick = tick;
        this.protectionMap = protectionMap;
        this.isWolfPack = isWolfPack;
    }

    /**
     * Returns an iterable of potential prey for a given predator.
     * Uses the size-indexed storage in Cell to prioritize larger prey (better ROI).
     */
    public Iterable<Organism> getPreyFor(Animal predator) {
        List<Organism> buffet = new ArrayList<>();
        SpeciesKey predKey = predator.getSpeciesKey();

        // Optimized: Check if the predator eats animals at all
        boolean eatsAnimals = matrix.hasAnimalPrey(predKey) || (isWolfPack && predKey.equals(SpeciesKey.WOLF));
        if (eatsAnimals) {
            // Priority 1: Large/Medium Herbivores (Better ROI)
            for (Animal p : cell.getHerbivores()) {
                if (p.isAlive() && (matrix.getChance(predKey, p.getSpeciesKey()) > 0)) {
                    if (!isProtected(p)) {
                        buffet.add(p);
                    }
                }
            }
            // Priority 2: Other Predators (if the matrix allows or pack hunt)
            for (Animal p : cell.getPredators()) {
                if (p == predator || !p.isAlive()) {
                    continue;
                }
                
                boolean canEat = matrix.getChance(predKey, p.getSpeciesKey()) > 0;
                
                // Special: Wolf Pack can hunt Bear
                if (!canEat && isWolfPack && predKey.equals(SpeciesKey.WOLF) && p.getSpeciesKey().equals(SpeciesKey.BEAR)) {
                    if (!p.isHibernating()) {
                        canEat = true;
                    }
                }

                if (canEat && !isProtected(p)) {
                    buffet.add(p);
                }
            }
            // Sort buffet by weight descending for ROI prioritization
            buffet.sort((o1, o2) -> Double.compare(o2.getWeight(), o1.getWeight()));
        }

        // 3. Check Caterpillar and Butterfly Biomass (Special cases)
        Biomass caterpillar = cell.getBiomass(SpeciesKey.CATERPILLAR);
        if (caterpillar != null && caterpillar.isAlive() && matrix.getChance(predKey, SpeciesKey.CATERPILLAR) > 0) {
            buffet.add(caterpillar);
        }

        Biomass butterfly = cell.getBiomass(SpeciesKey.BUTTERFLY);
        if (butterfly != null && butterfly.isAlive() && matrix.getChance(predKey, SpeciesKey.BUTTERFLY) > 0) {
            buffet.add(butterfly);
        }

        return buffet;
    }

    private boolean isProtected(Animal prey) {
        if (prey.isProtected(currentTick)) {
            return true;
        }
        Double hideChance = protectionMap.get(prey.getSpeciesKey());
        return hideChance != null && RandomUtils.nextDouble() < hideChance;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
    }

    public void markAsEaten(Organism prey) {
        // Handled by cell.removeAnimal under lock in FeedingService
    }
}
