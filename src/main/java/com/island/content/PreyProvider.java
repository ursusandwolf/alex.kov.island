package com.island.content;

import com.island.model.Cell;
import com.island.util.InteractionMatrix;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mediator that provides prey to predators within a single cell during a tick.
 * Optimized to avoid O(N^2) searches and handle "hiding" state.
 */
public class PreyProvider {
    private final Map<String, List<Animal>> speciesGroups;
    private final InteractionMatrix matrix;
    private final int currentTick;

    public PreyProvider(Cell cell, InteractionMatrix matrix, int currentTick) {
        this.matrix = matrix;
        this.currentTick = currentTick;
        // Group available (alive and not hiding) animals by species
        this.speciesGroups = cell.getAnimals().stream()
                .filter(a -> a.isAlive() && !a.isProtected(currentTick))
                .collect(Collectors.groupingBy(Animal::getSpeciesKey, Collectors.toCollection(ArrayList::new)));
    }

    /**
     * Provides an iterator for potential prey based on predator's diet and needs.
     */
    public Iterable<Animal> getPreyFor(Animal predator) {
        double foodNeeded = predator.getFoodForSaturation() - predator.getCurrentEnergy();
        if (foodNeeded <= 0) return Collections.emptyList();

        List<Animal> potentialTargets = new ArrayList<>();
        String predKey = predator.getSpeciesKey();

        // Get all species this predator can eat
        List<String> edibleSpecies = speciesGroups.keySet().stream()
                .filter(preyKey -> matrix.getChance(predKey, preyKey) > 0)
                .sorted(Comparator.comparingDouble(key -> -matrix.getChance(predKey, key))) // Prefer easier targets
                .toList();

        for (String preyKey : edibleSpecies) {
            List<Animal> available = speciesGroups.get(preyKey);
            if (available == null || available.isEmpty()) continue;

            int chance = matrix.getChance(predKey, preyKey);
            double avgWeight = available.get(0).getWeight();
            
            // Calculate how many we might need to satisfy hunger
            // Formula: (Needed / (Weight * Probability)) * SafetyFactor
            double expectedYieldPerPrey = avgWeight * (chance / 100.0);
            int countNeeded = (int) Math.ceil((foodNeeded / expectedYieldPerPrey) * 1.5);
            
            // Limit by actual availability
            int toProvide = Math.min(countNeeded, available.size());
            
            for (int i = 0; i < toProvide; i++) {
                potentialTargets.add(available.get(i));
            }
            
            // If we found enough potential targets to likely satisfy hunger, stop adding species
            if (potentialTargets.size() > 10) break; 
        }

        return potentialTargets;
    }

    public void markAsHiding(Animal prey) {
        prey.setHiding(true);
        // Remove from current provider pool so other predators don't see it this tick
        List<Animal> list = speciesGroups.get(prey.getSpeciesKey());
        if (list != null) {
            list.remove(prey);
        }
    }

    public void markAsEaten(Animal prey) {
        List<Animal> list = speciesGroups.get(prey.getSpeciesKey());
        if (list != null) {
            list.remove(prey);
        }
    }
}
