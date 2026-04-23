package com.island.content;

import lombok.Getter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

// Конфигуратор видов (Singleton)
@Getter
public final class SpeciesConfig {
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    private final Map<String, AnimalType> animalTypes = new HashMap<>();

    public static SpeciesConfig getInstance() { return INSTANCE; }

    private SpeciesConfig() {
        initPredators();
        initHerbivores();
    }

    private void initPredators() {
        animalTypes.put("wolf", new AnimalType("wolf", "Wolf", 50, 30, 3, 8, 10000, Map.of("rabbit", 60, "duck", 40)));
        animalTypes.put("fox", new AnimalType("fox", "Fox", 8, 30, 2, 2, 10000, Map.of("rabbit", 70, "duck", 50)));
    }

    private void initHerbivores() {
        animalTypes.put("rabbit", new AnimalType("rabbit", "Rabbit", 2, 150, 2, 0.45, 10000, null));
        animalTypes.put("duck", new AnimalType("duck", "Duck", 1, 200, 4, 0.15, 10000, Map.of("caterpillar", 90)));
        animalTypes.put("caterpillar", new AnimalType("caterpillar", "Caterpillar", 0.01, 1000, 0, 0, Integer.MAX_VALUE, null));
    }

    public AnimalType getAnimalType(String key) { return animalTypes.get(key); }
    public boolean hasSpecies(String key) { return animalTypes.containsKey(key); }
    public Set<String> getAllSpeciesKeys() { return animalTypes.keySet(); }

    public boolean isPredator(String key) {
        AnimalType type = animalTypes.get(key);
        return type != null && type.isPredator();
    }

    public int getHuntProbability(String predator, String prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }

    public boolean canEat(String predator, String prey) {
        return getHuntProbability(predator, prey) > 0;
    }

    public boolean rollHuntSuccess(String predator, String prey) {
        int prob = getHuntProbability(predator, prey);
        return prob > 0 && ThreadLocalRandom.current().nextInt(101) < prob;
    }
}
