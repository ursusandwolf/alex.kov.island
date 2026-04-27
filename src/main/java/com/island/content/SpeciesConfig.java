package com.island.content;

import lombok.Getter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Registry proxy for species configuration.
 * Refactored to delegate to SpeciesRegistry and SpeciesLoader (SRP).
 */
@Getter
public final class SpeciesConfig {
    private static final SpeciesConfig INSTANCE = new SpeciesConfig();
    private final SpeciesRegistry registry;

    private SpeciesConfig() {
        this.registry = new SpeciesLoader().load();
    }

    public static SpeciesConfig getInstance() { return INSTANCE; }

    public AnimalType getAnimalType(SpeciesKey key) {
        return registry.getAnimalType(key).orElse(null);
    }

    /**
     * Transitional method for String keys.
     */
    public AnimalType getAnimalType(String key) {
        try {
            return getAnimalType(SpeciesKey.fromCode(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    
    public Set<String> getAllSpeciesKeys() {
        Set<String> allKeys = registry.getAllAnimalKeys().stream()
                .map(SpeciesKey::getCode)
                .collect(Collectors.toSet());
        allKeys.add(SpeciesKey.PLANT.getCode());
        allKeys.add(SpeciesKey.CABBAGE.getCode());
        allKeys.add(SpeciesKey.CATERPILLAR.getCode());
        return allKeys;
    }

    public Set<SpeciesKey> getAllAnimalKeys() {
        return registry.getAllAnimalKeys();
    }

    public int getHuntProbability(String predator, String prey) {
        try {
            AnimalType type = getAnimalType(SpeciesKey.fromCode(predator));
            return (type != null) ? type.getHuntProbability(SpeciesKey.fromCode(prey)) : 0;
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public double getPlantWeight() { return registry.getPlantWeight(SpeciesKey.PLANT); }
    public int getPlantMaxCount() { return registry.getPlantMaxCount(SpeciesKey.PLANT); }
    public double getCabbageWeight() { return registry.getPlantWeight(SpeciesKey.CABBAGE); }
    public int getCabbageMaxCount() { return registry.getPlantMaxCount(SpeciesKey.CABBAGE); }
}
