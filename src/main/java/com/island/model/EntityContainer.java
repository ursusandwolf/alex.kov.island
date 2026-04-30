package com.island.model;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.SizeClass;
import com.island.content.SpeciesKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Pure storage component for a Cell.
 * Handles indexing by type, role, and size.
 * Optimized with LinkedHashSet for O(1) removal while maintaining order.
 */
public class EntityContainer {
    private final Map<AnimalType, Set<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, Set<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    private final Set<Animal> predators = new java.util.LinkedHashSet<>();
    private final Set<Animal> herbivores = new java.util.LinkedHashSet<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    
    private final Set<Animal> allAnimals = new java.util.LinkedHashSet<>();
    private final List<Biomass> allBiomass = new ArrayList<>();

    public void addAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        animalsByType.computeIfAbsent(type, k -> new LinkedHashSet<>()).add(animal);
        allAnimals.add(animal);
        
        if (animal.isAnimalPredator()) {
            predators.add(animal);
        } else {
            herbivores.add(animal);
        }

        SizeClass size = type.getSizeClass();
        animalsBySize.computeIfAbsent(size, k -> new LinkedHashSet<>()).add(animal);
    }

    public boolean removeAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        Set<Animal> typeSet = animalsByType.get(type);
        if (typeSet != null && typeSet.remove(animal)) {
            allAnimals.remove(animal);
            if (animal.isAnimalPredator()) {
                predators.remove(animal);
            } else {
                herbivores.remove(animal);
            }
            
            SizeClass size = type.getSizeClass();
            Set<Animal> sizeSet = animalsBySize.get(size);
            if (sizeSet != null) {
                sizeSet.remove(animal);
            }
            return true;
        }
        return false;
    }

    public Set<Animal> getAllAnimals() {
        return allAnimals;
    }

    public Set<Animal> getPredators() {
        return predators;
    }

    public Set<Animal> getHerbivores() {
        return herbivores;
    }

    public Set<Animal> getByType(AnimalType type) {
        return animalsByType.getOrDefault(type, Collections.emptySet());
    }

    public Set<Animal> getBySize(SizeClass size) {
        return animalsBySize.getOrDefault(size, Collections.emptySet());
    }

    public int countByType(AnimalType type) {
        Set<Animal> set = animalsByType.get(type);
        return set != null ? set.size() : 0;
    }

    public int countBySpecies(SpeciesKey key) {
        int count = 0;
        // This is still O(T) where T is number of types
        for (Map.Entry<AnimalType, Set<Animal>> entry : animalsByType.entrySet()) {
            if (entry.getKey().getSpeciesKey().equals(key)) {
                count += entry.getValue().size();
            }
        }
        
        Biomass b = biomassBySpecies.get(key);
        if (b != null) {
            count += (int) b.getBiomass();
        }
        
        return count;
    }

    // ... biomass methods ...

    public void addBiomass(Biomass b) {
        biomassBySpecies.put(b.getSpeciesKey(), b);
        allBiomass.add(b);
    }

    public boolean removeBiomass(Biomass b) {
        if (biomassBySpecies.remove(b.getSpeciesKey()) != null) {
            allBiomass.remove(b);
            return true;
        }
        return false;
    }

    public List<Biomass> getAllBiomass() {
        return allBiomass;
    }

    public Biomass getBiomass(SpeciesKey key) {
        return biomassBySpecies.get(key);
    }
}
