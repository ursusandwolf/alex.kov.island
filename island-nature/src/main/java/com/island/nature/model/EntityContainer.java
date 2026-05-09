package com.island.nature.model;

import com.island.engine.ecs.EntityArchetype;
import com.island.engine.ecs.EntityQuery;
import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;

public class EntityContainer {
    private final Configuration config;
    private final Map<AnimalType, List<Animal>> animalsByType = new HashMap<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new LinkedHashMap<>();
    
    // Archetype grouping for optimized querying
    private final Map<EntityArchetype, List<Organism>> entitiesByArchetype = new LinkedHashMap<>();
    
    // Pre-calculated counts to avoid O(N) iteration
    private int animalCount = 0;

    public EntityContainer(Configuration config) {
        this.config = config;
    }

    public void addAnimal(Animal animal) {
        AnimalType type = animal.getAnimalType();
        animalsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(animal);
        animalCount++;
        
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
            animalCount--;
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
        return animalCount;
    }

    public int getBiomassCount() {
        return biomassBySpecies.size();
    }

    public int getEntityCount() {
        return animalCount + biomassBySpecies.size();
    }

    public List<Animal> getByType(AnimalType type) {
        List<Animal> list = animalsByType.get(type);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<Animal> getAllAnimals() {
        List<Animal> all = new ArrayList<>(animalCount);
        for (List<Animal> list : animalsByType.values()) {
            all.addAll(list);
        }
        return all;
    }

    public List<Biomass> getAllBiomass() {
        return new ArrayList<>(biomassBySpecies.values());
    }

    public int countByType(AnimalType type) {
        List<Animal> list = animalsByType.get(type);
        return list != null ? list.size() : 0;
    }

    public int countBySpecies(SpeciesKey key) {
        // Optimization: try to find by type first if it's an animal
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
        for (List<Animal> list : animalsByType.values()) {
            for (int i = 0; i < list.size(); i++) {
                action.accept(list.get(i));
            }
        }
    }

    public void forEachBiomass(Consumer<Biomass> action) {
        for (Biomass b : biomassBySpecies.values()) {
            action.accept(b);
        }
    }

    public void forEachEntity(Consumer<Organism> action) {
        forEachAnimal(action::accept);
        for (Biomass b : biomassBySpecies.values()) {
            action.accept((Organism) b);
        }
    }

    public void forEachMatching(EntityQuery<Organism> query, Consumer<Organism> action) {
        for (Map.Entry<EntityArchetype, List<Organism>> entry : entitiesByArchetype.entrySet()) {
            if (query.matches(entry.getKey())) {
                List<Organism> entities = entry.getValue();
                for (int i = 0; i < entities.size(); i++) {
                    action.accept(entities.get(i));
                }
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
                    animalCount--;
                    
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
