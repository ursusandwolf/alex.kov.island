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
import lombok.Getter;
import static com.island.config.SimulationConstants.SCALE_1M;

public class EntityContainer {
    private final Map<AnimalType, Set<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, Set<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    @Getter private final Set<Animal> predators = new LinkedHashSet<>();
    @Getter private final Set<Animal> herbivores = new LinkedHashSet<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    @Getter private final Set<Animal> allAnimals = new LinkedHashSet<>();
    @Getter private final List<Biomass> allBiomass = new ArrayList<>();

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
        for (Map.Entry<AnimalType, Set<Animal>> entry : animalsByType.entrySet()) {
            if (entry.getKey().getSpeciesKey().equals(key)) {
                count += entry.getValue().size();
            }
        }
        Biomass b = biomassBySpecies.get(key);
        if (b != null) {
            count += (int) (b.getBiomass() / SCALE_1M);
        }
        return count;
    }

    public void removeDeadAnimals(java.util.function.Consumer<Animal> onRemoved) {
        java.util.Iterator<Animal> it = allAnimals.iterator();
        while (it.hasNext()) {
            Animal a = it.next();
            if (!a.isAlive()) {
                it.remove();
                
                // Remove from other indices
                AnimalType type = a.getAnimalType();
                Set<Animal> typeSet = animalsByType.get(type);
                if (typeSet != null) {
                    typeSet.remove(a);
                }
                
                if (a.isAnimalPredator()) {
                    predators.remove(a);
                } else {
                    herbivores.remove(a);
                }
                
                SizeClass size = type.getSizeClass();
                Set<Animal> sizeSet = animalsBySize.get(size);
                if (sizeSet != null) {
                    sizeSet.remove(a);
                }
                
                if (onRemoved != null) {
                    onRemoved.accept(a);
                }
            }
        }
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

    public Biomass getBiomass(SpeciesKey key) {
        return biomassBySpecies.get(key);
    }
}
