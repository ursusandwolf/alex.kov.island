package com.island.content;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable registry of species data.
 */
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes;
    private final Map<SpeciesKey, Double> plantWeights;
    private final Map<SpeciesKey, Integer> plantMaxCounts;

    public SpeciesRegistry(Map<SpeciesKey, AnimalType> animalTypes, 
                           Map<SpeciesKey, Double> plantWeights, 
                           Map<SpeciesKey, Integer> plantMaxCounts) {
        this.animalTypes = Collections.unmodifiableMap(new EnumMap<>(animalTypes));
        this.plantWeights = Collections.unmodifiableMap(new EnumMap<>(plantWeights));
        this.plantMaxCounts = Collections.unmodifiableMap(new EnumMap<>(plantMaxCounts));
    }

    public Optional<AnimalType> getAnimalType(SpeciesKey key) {
        return Optional.ofNullable(animalTypes.get(key));
    }

    public double getPlantWeight(SpeciesKey key) {
        return plantWeights.getOrDefault(key, 0.0);
    }

    public int getPlantMaxCount(SpeciesKey key) {
        return plantMaxCounts.getOrDefault(key, 0);
    }

    public Set<SpeciesKey> getAllAnimalKeys() {
        return animalTypes.keySet();
    }
}
