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
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a single cell on the island grid.
 * Optimized with indexed storage by type, role, and size for O(1) access.
 */
public class Cell {
    private final int x;
    private final int y;
    private final Island island;
    
    // Indexed Storage
    private final Map<AnimalType, List<Animal>> animalsByType = new HashMap<>();
    private final Map<SizeClass, List<Animal>> animalsBySize = new EnumMap<>(SizeClass.class);
    private final List<Animal> predators = new ArrayList<>();
    private final List<Animal> herbivores = new ArrayList<>();
    private final Map<SpeciesKey, Biomass> biomassBySpecies = new EnumMap<>(SpeciesKey.class);
    
    // Flat list for general iteration
    private final List<Animal> allAnimals = new ArrayList<>();
    private final List<Biomass> allBiomass = new ArrayList<>();
    
    private final ReentrantLock lock = new ReentrantLock();

    public Cell(int x, int y, Island island) { 
        this.x = x; 
        this.y = y; 
        this.island = island;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Island getIsland() {
        return island;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public String getCoordinates() {
        return x + "," + y;
    }

    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            AnimalType type = animal.getAnimalType();
            List<Animal> typeList = animalsByType.computeIfAbsent(type, k -> new ArrayList<>());
            
            if (typeList.size() >= animal.getMaxPerCell()) {
                return false;
            }
            
            typeList.add(animal);
            allAnimals.add(animal);
            
            // Index by role
            if (animal.isAnimalPredator()) {
                predators.add(animal);
            } else {
                herbivores.add(animal);
            }

            // Index by size
            SizeClass size = type.getSizeClass();
            animalsBySize.computeIfAbsent(size, k -> new ArrayList<>()).add(animal);
            
            island.onOrganismAdded(animal.getSpeciesKey());
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try { 
            AnimalType type = animal.getAnimalType();
            List<Animal> typeList = animalsByType.get(type);
            if (typeList != null && typeList.remove(animal)) {
                allAnimals.remove(animal);
                if (animal.isAnimalPredator()) {
                    predators.remove(animal);
                } else {
                    herbivores.remove(animal);
                }
                
                SizeClass size = type.getSizeClass();
                List<Animal> sizeList = animalsBySize.get(size);
                if (sizeList != null) {
                    sizeList.remove(animal);
                }

                island.onOrganismRemoved(animal.getSpeciesKey());
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getAnimals() {
        return new ArrayList<>(allAnimals);
    }

    public List<Animal> getPredators() {
        return new ArrayList<>(predators);
    }

    public List<Animal> getHerbivores() {
        return new ArrayList<>(herbivores);
    }

    public List<Animal> getAnimalsByType(AnimalType type) {
        lock.lock();
        try {
            List<Animal> list = animalsByType.get(type);
            return list != null ? new ArrayList<>(list) : Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getAnimalsBySize(SizeClass size) {
        lock.lock();
        try {
            List<Animal> list = animalsBySize.get(size);
            return list != null ? new ArrayList<>(list) : Collections.emptyList();
        } finally {
            lock.unlock();
        }
    }

    public int countAnimalsByType(AnimalType type) {
        lock.lock();
        try {
            List<Animal> list = animalsByType.get(type);
            return list != null ? list.size() : 0;
        } finally {
            lock.unlock();
        }
    }

    public int getAnimalCount() {
        return allAnimals.size();
    }

    public boolean addBiomass(Biomass b) {
        lock.lock();
        try {
            SpeciesKey key = b.getSpeciesKey();
            Biomass existing = biomassBySpecies.get(key);
            if (existing != null) {
                existing.addBiomass(b.getBiomass());
                return true;
            } else {
                biomassBySpecies.put(key, b);
                allBiomass.add(b);
                return true;
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean addBiomass(SpeciesKey key, double amount) {
        lock.lock();
        try {
            Biomass existing = biomassBySpecies.get(key);
            if (existing != null) {
                existing.addBiomass(amount);
                return true;
            }
            return false; // Cannot add mass if container doesn't exist in this cell
        } finally {
            lock.unlock();
        }
    }

    public boolean removeBiomass(Biomass b) {
        lock.lock();
        try {
            if (biomassBySpecies.remove(b.getSpeciesKey()) != null) {
                allBiomass.remove(b);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public List<Biomass> getBiomassContainers() {
        return new ArrayList<>(allBiomass);
    }

    public Biomass getBiomass(SpeciesKey key) {
        return biomassBySpecies.get(key);
    }

    public int getPlantCount() { 
        double total = 0;
        for (Biomass b : allBiomass) {
            total += b.getBiomass();
        }
        return (int) total; 
    }

    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            List<Animal> toRemove = new ArrayList<>();
            for (Animal a : allAnimals) {
                if (!a.isAlive()) {
                    toRemove.add(a);
                }
            }
            for (Animal a : toRemove) {
                removeAnimal(a);
            }
        } finally {
            lock.unlock();
        }
    }

    public String getStatistics() {
        return String.format("Cell[%d,%d]: Животные=%d, Биомасса=%d", x, y, allAnimals.size(), (int) getPlantCount());
    }
}
