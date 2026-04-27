package com.island.util;

import com.island.content.SpeciesKey;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionMatrix {
    private final Map<SpeciesKey, Map<SpeciesKey, Integer>> matrix = new ConcurrentHashMap<>();

    public void setChance(SpeciesKey predator, SpeciesKey prey, int chance) {
        matrix.computeIfAbsent(predator, k -> new ConcurrentHashMap<>()).put(prey, chance);
    }

    public int getChance(SpeciesKey predator, SpeciesKey prey) {
        return matrix.getOrDefault(predator, new ConcurrentHashMap<>())
                     .getOrDefault(prey, 0);
    }
}
