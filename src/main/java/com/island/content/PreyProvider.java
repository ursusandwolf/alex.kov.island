package com.island.content;

import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import com.island.content.plants.Plant;
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
     * Uses prioritized buckets (Herbivores, other Predators, Biomass).
     */
    public Iterable<Organism> getPreyFor(Animal predator) {
        List<Organism> buffet = new ArrayList<>();
        SpeciesKey predKey = predator.getSpeciesKey();

        // 1. Check Herbivores (Primary target)
        List<Animal> herbivores = new ArrayList<>(cell.getHerbivores());
        Collections.shuffle(herbivores, ThreadLocalRandom.current());
        for (Animal h : herbivores) {
            if (h.isAlive() && matrix.getChance(predKey, h.getSpeciesKey()) > 0) {
                if (isProtected(h)) continue;
                buffet.add(h);
            }
        }

        // 2. Check other Predators (Intraspecies or interspecies competition)
        List<Animal> predators = new ArrayList<>(cell.getPredators());
        Collections.shuffle(predators, ThreadLocalRandom.current());
        for (Animal p : predators) {
            if (p != predator && p.isAlive() && matrix.getChance(predKey, p.getSpeciesKey()) > 0) {
                if (isProtected(p)) continue;
                buffet.add(p);
            }
        }

        // 3. Check Caterpillar Biomass (Special case)
        Plant caterpillar = cell.getPlant(SpeciesKey.CATERPILLAR);
        if (caterpillar != null && caterpillar.isAlive() && matrix.getChance(predKey, SpeciesKey.CATERPILLAR) > 0) {
             buffet.add(caterpillar);
        }

        return buffet;
    }

    private boolean isProtected(Animal prey) {
        if (prey.isProtected(currentTick)) return true;
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
