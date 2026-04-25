package com.island.util;

import com.island.content.AnimalType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InteractionMatrix {
    private final Map<String, Map<String, Integer>> matrix = new ConcurrentHashMap<>();

    public void setChance(String predator, String prey, int chance) {
        matrix.computeIfAbsent(predator, k -> new ConcurrentHashMap<>()).put(prey, chance);
    }

    public int getChance(String predator, String prey) {
        return matrix.getOrDefault(predator, new ConcurrentHashMap<>())
                     .getOrDefault(prey, 0);
    }
}
