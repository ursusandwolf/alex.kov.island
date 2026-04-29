package com.island.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable registry of species data.
 */
@Getter
@RequiredArgsConstructor
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes;
    private final Map<SpeciesKey, Double> plantWeights;
    private final Map<SpeciesKey, Integer> plantMaxCounts;
    private final Map<SpeciesKey, Integer> plantSpeeds;

    public Optional<AnimalType> getAnimalType(SpeciesKey key) {
        return Optional.ofNullable(animalTypes.get(key));
    }

    public double getPlantWeight(SpeciesKey key) {
        return plantWeights.getOrDefault(key, 0.0);
    }

    public int getPlantMaxCount(SpeciesKey key) {
        return plantMaxCounts.getOrDefault(key, 0);
    }

    public int getPlantSpeed(SpeciesKey key) {
        return plantSpeeds.getOrDefault(key, 0);
    }

    public Set<SpeciesKey> getAllAnimalKeys() {
        return animalTypes.keySet();
    }

    public Set<String> getAllSpeciesCodes() {
        Set<String> allCodes = new HashSet<>();
        animalTypes.keySet().forEach(k -> allCodes.add(k.getCode()));
        plantWeights.keySet().forEach(k -> allCodes.add(k.getCode()));
        return allCodes;
    }

    public int getHuntProbability(SpeciesKey predator, SpeciesKey prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }
}
