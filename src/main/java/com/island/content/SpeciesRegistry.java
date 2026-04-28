package com.island.content;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable registry of species data.
 */
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes;
    private final Map<SpeciesKey, Double> plantWeights;
    private final Map<SpeciesKey, Integer> plantMaxCounts;
    private final Map<SpeciesKey, Integer> plantSpeeds;

    public SpeciesRegistry(Map<SpeciesKey, AnimalType> animalTypes, 
                           Map<SpeciesKey, Double> plantWeights, 
                           Map<SpeciesKey, Integer> plantMaxCounts,
                           Map<SpeciesKey, Integer> plantSpeeds) {
        this.animalTypes = Collections.unmodifiableMap(new HashMap<>(animalTypes));
        this.plantWeights = Collections.unmodifiableMap(new HashMap<>(plantWeights));
        this.plantMaxCounts = Collections.unmodifiableMap(new HashMap<>(plantMaxCounts));
        this.plantSpeeds = Collections.unmodifiableMap(new HashMap<>(plantSpeeds));
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

    public int getPlantSpeed(SpeciesKey key) {
        return plantSpeeds.getOrDefault(key, 0);
    }

    public Set<SpeciesKey> getAllAnimalKeys() {
        return animalTypes.keySet();
    }

    public Set<String> getAllSpeciesCodes() {
        Set<String> allCodes = animalTypes.keySet().stream()
                .map(SpeciesKey::getCode)
                .collect(Collectors.toSet());
        allCodes.add(SpeciesKey.PLANT.getCode());
        allCodes.add(SpeciesKey.CABBAGE.getCode());
        allCodes.add(SpeciesKey.CATERPILLAR.getCode());
        return allCodes;
    }

    public int getHuntProbability(SpeciesKey predator, SpeciesKey prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }
}
