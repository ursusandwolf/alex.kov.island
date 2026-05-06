package com.island.nature.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Immutable registry of species data.
 */
@Getter
public class SpeciesRegistry {
    private final Map<SpeciesKey, AnimalType> animalTypes;
    private final Map<SpeciesKey, AnimalType> biomassTypes;
    private final Map<String, SpeciesKey> keyRegistry;

    public SpeciesRegistry(Map<SpeciesKey, AnimalType> animalTypes, 
                           Map<SpeciesKey, AnimalType> biomassTypes,
                           Map<String, SpeciesKey> keyRegistry) {
        this.animalTypes = Collections.unmodifiableMap(new HashMap<>(animalTypes));
        this.biomassTypes = Collections.unmodifiableMap(new HashMap<>(biomassTypes));
        this.keyRegistry = Collections.unmodifiableMap(new HashMap<>(keyRegistry));
    }

    public Optional<SpeciesKey> getKey(String code) {
        return Optional.ofNullable(keyRegistry.get(code.toLowerCase()));
    }

    public Collection<SpeciesKey> getAllSpeciesKeys() {
        return keyRegistry.values();
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
