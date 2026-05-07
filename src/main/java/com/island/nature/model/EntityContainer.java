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
    private final Map<AnimalType, List<Animal>> animalsByType = new HashMap<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new HashMap<>();
    
    // Archetype grouping for optimized querying
    private final Map<EntityArchetype, List<Organism>> entitiesByArchetype = new HashMap<>();

    public EntityContainer(Configuration config) {
        this.config = config;
    }

    public void addAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(animal);
        
        // Update archetype index
        EntityArchetype archetype = animal.getArchetype();
        if (archetype != null) {
            entitiesByArchetype.computeIfAbsent(archetype, k -> new ArrayList<>()).add(animal);
        }
    }

    public boolean removeAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        List<Animal> typeList = animalsByType.get(type);
        if (typeList != null && typeList.remove(animal)) {
            // Update archetype index
            EntityArchetype archetype = animal.getArchetype();
            if (archetype != null) {
                List<Organism> archList = entitiesByArchetype.get(archetype);
                if (archList != null) {
                    archList.remove(animal);
                    if (archList.isEmpty()) {
                        entitiesByArchetype.remove(archetype);
                    }
                }
            }
            return true;
        }
        return false;
    }

    public int getAnimalCount() {
        int count = 0;
        for (List<Animal> list : animalsByType.values()) {
            count += list.size();
        }
        return count;
    }

    public int getBiomassCount() {
        return biomassBySpecies.size();
    }

    public int getEntityCount() {
        return getAnimalCount() + getBiomassCount();
    }

    public List<Animal> getByType(AnimalType type) {
        List<Animal> list = animalsByType.get(type);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<Animal> getAllAnimals() {
        List<Animal> all = new ArrayList<>();
        animalsByType.values().forEach(all::addAll);
        return all;
    }

    public List<Biomass> getAllBiomass() {
        return List.copyOf(biomassBySpecies.values());
    }

    public int countByType(AnimalType type) {
        List<Animal> list = animalsByType.get(type);
        return list != null ? list.size() : 0;
    }

    public int countBySpecies(SpeciesKey key) {
        int count = 0;
        for (Map.Entry<AnimalType, List<Animal>> entry : animalsByType.entrySet()) {
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
        animalsByType.values().forEach(list -> list.forEach(action));
    }

    public void forEachBiomass(Consumer<Biomass> action) {
        biomassBySpecies.values().forEach(action);
    }

    public void forEachEntity(Consumer<Organism> action) {
        forEachAnimal(action::accept);
        biomassBySpecies.values().forEach(b -> action.accept((Organism) b));
    }

    public void forEachMatching(EntityQuery<Organism> query, Consumer<Organism> action) {
        for (Map.Entry<EntityArchetype, List<Organism>> entry : entitiesByArchetype.entrySet()) {
            if (query.matches(entry.getKey())) {
                entry.getValue().forEach(action);
            }
        }
    }

    public void removeDeadAnimals(Consumer<Animal> onRemoved) {
        for (List<Animal> list : animalsByType.values()) {
            Iterator<Animal> it = list.iterator();
            while (it.hasNext()) {
                Animal a = it.next();
                if (!a.isAlive()) {
                    it.remove();
                    
                    // Update archetype index
                    EntityArchetype archetype = a.getArchetype();
                    if (archetype != null) {
                        List<Organism> archList = entitiesByArchetype.get(archetype);
                        if (archList != null) {
                            archList.remove(a);
                            if (archList.isEmpty()) {
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
    }

    public void addBiomass(Biomass b) {
        biomassBySpecies.put(b.getSpeciesKey(), b);
        
        // Update archetype index
        EntityArchetype archetype = b.getArchetype();
        if (archetype != null) {
            entitiesByArchetype.computeIfAbsent(archetype, k -> new ArrayList<>()).add(b);
        }
    }

    public Biomass getBiomass(SpeciesKey key) {
        return biomassBySpecies.get(key);
    }
}
