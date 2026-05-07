package com.island.nature.model;

import com.island.engine.ecs.EntityArchetype;
import com.island.engine.ecs.EntityQuery;
import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SizeClass;
import com.island.nature.entities.core.SpeciesKey;

public class EntityContainer {
    private final Configuration config;
    private final Map<AnimalType, Set<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, Set<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    private final Set<Animal> predators = new LinkedHashSet<>();
    private final Set<Animal> herbivores = new LinkedHashSet<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    private final Set<Animal> allAnimals = new LinkedHashSet<>();
    private final List<Biomass> allBiomass = new ArrayList<>();
    
    // Archetype grouping for optimized querying
    private final Map<EntityArchetype, Set<Organism>> entitiesByArchetype = new HashMap<>();

    public EntityContainer(Configuration config) {
        this.config = config;
    }

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
        
        // Update archetype index
        EntityArchetype archetype = animal.getArchetype();
        if (archetype != null) {
            entitiesByArchetype.computeIfAbsent(archetype, k -> new LinkedHashSet<>()).add(animal);
        }
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
            
            // Update archetype index
            EntityArchetype archetype = animal.getArchetype();
            if (archetype != null) {
                Set<Organism> archSet = entitiesByArchetype.get(archetype);
                if (archSet != null) {
                    archSet.remove(animal);
                    if (archSet.isEmpty()) {
                        entitiesByArchetype.remove(archetype);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public Set<Animal> getByType(AnimalType type) {
        Set<Animal> set = animalsByType.get(type);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    public Set<Animal> getBySize(SizeClass size) {
        Set<Animal> set = animalsBySize.get(size);
        return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
    }

    public Set<Animal> getPredators() {
        return Collections.unmodifiableSet(predators);
    }

    public Set<Animal> getHerbivores() {
        return Collections.unmodifiableSet(herbivores);
    }

    public Set<Animal> getAllAnimals() {
        return Collections.unmodifiableSet(allAnimals);
    }

    public List<Biomass> getAllBiomass() {
        return Collections.unmodifiableList(allBiomass);
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
            count += (int) (b.getBiomass() / config.getScale1M());
        }
        return count;
    }

    public void forEachAnimal(Consumer<Animal> action) {
        allAnimals.forEach(action);
    }

    public void forEachBiomass(Consumer<Biomass> action) {
        allBiomass.forEach(action);
    }

    public void forEachEntity(Consumer<Organism> action) {
        allAnimals.forEach(action);
        allBiomass.forEach(action);
    }

    public void forEachMatching(EntityQuery<Organism> query, Consumer<Organism> action) {
        for (Map.Entry<EntityArchetype, Set<Organism>> entry : entitiesByArchetype.entrySet()) {
            if (query.matches(entry.getKey())) {
                entry.getValue().forEach(action);
            }
        }
    }

    public void removeDeadAnimals(Consumer<Animal> onRemoved) {
        Iterator<Animal> it = allAnimals.iterator();
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

                // Update archetype index
                EntityArchetype archetype = a.getArchetype();
                if (archetype != null) {
                    Set<Organism> archSet = entitiesByArchetype.get(archetype);
                    if (archSet != null) {
                        archSet.remove(a);
                        if (archSet.isEmpty()) {
                            entitiesByArchetype.remove(archetype);
                        }
                    }
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
        
        // Update archetype index
        EntityArchetype archetype = b.getArchetype();
        if (archetype != null) {
            entitiesByArchetype.computeIfAbsent(archetype, k -> new LinkedHashSet<>()).add(b);
        }
    }

    public Biomass getBiomass(SpeciesKey key) {
        return biomassBySpecies.get(key);
    }
}
