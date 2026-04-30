package com.island.content;

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
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes;
    private final Map<SpeciesKey, AnimalType> biomassTypes;

    public SpeciesRegistry(Map<SpeciesKey, AnimalType> animalTypes, Map<SpeciesKey, AnimalType> biomassTypes) {
        this.animalTypes = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(animalTypes));
        this.biomassTypes = java.util.Collections.unmodifiableMap(new java.util.HashMap<>(biomassTypes));
    }

    public Optional<AnimalType> getAnimalType(SpeciesKey key) {
        return Optional.ofNullable(animalTypes.get(key));
    }

    public Optional<AnimalType> getBiomassType(SpeciesKey key) {
        return Optional.ofNullable(biomassTypes.get(key));
    }

    public Optional<AnimalType> getAnyType(SpeciesKey key) {
        AnimalType type = animalTypes.get(key);
        return (type != null) ? Optional.of(type) : Optional.ofNullable(biomassTypes.get(key));
    }

    public long getPlantWeight(SpeciesKey key) {
        return Optional.ofNullable(biomassTypes.get(key)).map(AnimalType::getWeight).orElse(0L);
    }

    public int getPlantMaxCount(SpeciesKey key) {
        return Optional.ofNullable(biomassTypes.get(key)).map(AnimalType::getMaxPerCell).orElse(0);
    }

    public int getPlantSpeed(SpeciesKey key) {
        return Optional.ofNullable(biomassTypes.get(key)).map(AnimalType::getSpeed).orElse(0);
    }

    public Set<SpeciesKey> getAllAnimalKeys() {
        return animalTypes.keySet();
    }

    public Set<SpeciesKey> getAllBiomassKeys() {
        return biomassTypes.keySet();
    }

    public Set<String> getAllSpeciesCodes() {
        Set<String> allCodes = new HashSet<>();
        animalTypes.keySet().forEach(k -> allCodes.add(k.getCode()));
        biomassTypes.keySet().forEach(k -> allCodes.add(k.getCode()));
        return allCodes;
    }

    public int getHuntProbability(SpeciesKey predator, SpeciesKey prey) {
        AnimalType type = animalTypes.get(predator);
        return (type != null) ? type.getHuntProbability(prey) : 0;
    }
}
