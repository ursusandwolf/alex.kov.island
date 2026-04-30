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

/**
 * Pure storage component for a Cell.
 * Handles indexing by type, role, and size.
 * Not thread-safe; synchronization must be managed by the caller.
 */
public class EntityContainer {
    private final Map<AnimalType, List<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, List<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    private final List<Animal> predators = new ArrayList<>();
    private final List<Animal> herbivores = new ArrayList<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    
    private final List<Animal> allAnimals = new ArrayList<>();
    private final List<Biomass> allBiomass = new ArrayList<>();

    public void addAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(animal);
        allAnimals.add(animal);
        
        if (animal.isAnimalPredator()) {
            predators.add(animal);
        } else {
            herbivores.add(animal);
        }

        SizeClass size = type.getSizeClass();
        animalsBySize.computeIfAbsent(size, k -> new ArrayList<>()).add(animal);
    }

    public boolean removeAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        List<Animal> typeList = animalsByType.get(type);
        if (typeList != null && fastRemove(typeList, animal)) {
            fastRemove(allAnimals, animal);
            if (animal.isAnimalPredator()) {
                fastRemove(predators, animal);
            } else {
                fastRemove(herbivores, animal);
            }
            
            SizeClass size = type.getSizeClass();
            List<Animal> sizeList = animalsBySize.get(size);
            if (sizeList != null) {
                fastRemove(sizeList, animal);
            }
            return true;
        }
        return false;
    }

    private boolean fastRemove(List<Animal> list, Animal animal) {
        int index = list.indexOf(animal);
        if (index == -1) {
            return false;
        }
        int lastIndex = list.size() - 1;
        if (index != lastIndex) {
            list.set(index, list.get(lastIndex));
        }
        list.remove(lastIndex);
        return true;
    }

    public List<Animal> getAllAnimals() {
        return allAnimals;
    }

    public List<Animal> getPredators() {
        return predators;
    }

    public List<Animal> getHerbivores() {
        return herbivores;
    }

    public List<Animal> getByType(AnimalType type) {
        return animalsByType.getOrDefault(type, Collections.emptyList());
    }

    public List<Animal> getBySize(SizeClass size) {
        return animalsBySize.getOrDefault(size, Collections.emptyList());
    }

    public int countByType(AnimalType type) {
        List<Animal> list = animalsByType.get(type);
        return list != null ? list.size() : 0;
    }

    public int countBySpecies(SpeciesKey key) {
        // Count in animals
        int count = 0;
        for (Map.Entry<AnimalType, List<Animal>> entry : animalsByType.entrySet()) {
            if (entry.getKey().getSpeciesKey().equals(key)) {
                count += entry.getValue().size();
            }
        }
        
        // Count in biomass (as mass unit)
        Biomass b = biomassBySpecies.get(key);
        if (b != null) {
            count += (int) b.getBiomass();
        }
        
        return count;
    }

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
