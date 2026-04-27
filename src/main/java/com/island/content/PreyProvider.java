package com.island.content;

import com.island.content.animals.herbivores.Caterpillar;
import com.island.content.plants.Plant;
import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Optimized food provider that simulates a mixed "buffet" with competition.
 * Refactored to use bucketed prey sources (O(1) access) instead of linear scans and shuffles.
 */
public class PreyProvider {
    private final Cell cell;
    private final InteractionMatrix matrix;
    private final int currentTick;
    private final Map<SpeciesKey, Double> protectionMap;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int tick, Map<SpeciesKey, Double> protectionMap) {
        this.cell = cell;
        this.matrix = matrix;
        this.currentTick = tick;
        this.protectionMap = protectionMap;
    }

    /**
     * Returns an iterable of potential prey for a given predator.
     * Uses the size-indexed storage in Cell to prioritize larger prey (better ROI).
     */
    public Iterable<Organism> getPreyFor(Animal predator) {
        List<Organism> buffet = new ArrayList<>();
        SpeciesKey predKey = predator.getSpeciesKey();

        // 1. Check Animals by size (Descending order for better ROI)
        SizeClass[] sizes = SizeClass.values();
        for (int i = sizes.length - 1; i >= 0; i--) {
            SizeClass size = sizes[i];
            List<Animal> potentialPrey = cell.getAnimalsBySize(size);
            for (Animal p : potentialPrey) {
                if (p != predator && p.isAlive() && matrix.getChance(predKey, p.getSpeciesKey()) > 0) {
                    if (!isProtected(p)) {
                        buffet.add(p);
                    }
                }
            }
        }

        // 2. Check Caterpillar Biomass (Special case)
        Plant caterpillar = cell.getPlant(SpeciesKey.CATERPILLAR);
        if (caterpillar != null && caterpillar.isAlive() && matrix.getChance(predKey, SpeciesKey.CATERPILLAR) > 0) {
            buffet.add(caterpillar);
        }

        return buffet;
    }

    private boolean isProtected(Animal prey) {
        if (prey.isProtected(currentTick)) {
            return true;
        }
        Double hideChance = protectionMap.get(prey.getSpeciesKey());
        return hideChance != null && ThreadLocalRandom.current().nextDouble() < hideChance;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
    }

    public void markAsEaten(Organism prey) {
        // Handled by cell.removeAnimal under lock in FeedingService
    }
}
