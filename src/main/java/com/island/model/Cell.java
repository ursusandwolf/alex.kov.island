package com.island.model;

import com.island.content.Animal;
import com.island.content.SpeciesKey;
import com.island.content.plants.Plant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a single cell on the island grid.
 * Optimized with indexed storage by species and role for O(1) access and efficient feeding.
 */
public class Cell {
    private final int x, y;
    private final Island island;
    
    // Indexed Storage
    private final Map<SpeciesKey, List<Animal>> animalsBySpecies = new EnumMap<>(SpeciesKey.class);
    private final List<Animal> predators = new ArrayList<>();
    private final List<Animal> herbivores = new ArrayList<>();
    private final Map<SpeciesKey, Plant> plantsBySpecies = new EnumMap<>(SpeciesKey.class);
    
    // Flat lists for general iteration if needed
    private final List<Animal> allAnimals = new ArrayList<>();
    private final List<Plant> allPlants = new ArrayList<>();
    
    private final ReentrantLock lock = new ReentrantLock();

    public Cell(int x, int y, Island island) { 
        this.x = x; 
        this.y = y; 
        this.island = island;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public Island getIsland() { return island; }
    public ReentrantLock getLock() { return lock; }

    public String getCoordinates() { return x + "," + y; }

    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            SpeciesKey key = animal.getSpeciesKey();
            List<Animal> speciesList = animalsBySpecies.computeIfAbsent(key, k -> new ArrayList<>());
            
            if (speciesList.size() >= animal.getMaxPerCell()) return false;
            
            speciesList.add(animal);
            allAnimals.add(animal);
            
            if (animal.isAnimalPredator()) {
                predators.add(animal);
            } else {
                herbivores.add(animal);
            }
            
            island.onOrganismAdded(key);
            return true;
        } finally { lock.unlock(); }
    }

    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try { 
            SpeciesKey key = animal.getSpeciesKey();
            List<Animal> speciesList = animalsBySpecies.get(key);
            if (speciesList != null && speciesList.remove(animal)) {
                allAnimals.remove(animal);
                if (animal.isAnimalPredator()) {
                    predators.remove(animal);
                } else {
                    herbivores.remove(animal);
                }
                island.onOrganismRemoved(key);
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public List<Animal> getAnimals() { return allAnimals; }
    public List<Animal> getPredators() { return predators; }
    public List<Animal> getHerbivores() { return herbivores; }

    public List<Animal> getAnimalsBySpecies(SpeciesKey key) {
        lock.lock();
        try {
            List<Animal> list = animalsBySpecies.get(key);
            return list != null ? new ArrayList<>(list) : Collections.emptyList();
        } finally { lock.unlock(); }
    }

    public int countAnimalsBySpecies(SpeciesKey key) {
        lock.lock();
        try {
            List<Animal> list = animalsBySpecies.get(key);
            return list != null ? list.size() : 0;
        } finally { lock.unlock(); }
    }

    public int getAnimalCount() { return allAnimals.size(); }

    public boolean addPlant(Plant plant) {
        lock.lock();
        try {
            SpeciesKey key = plant.getSpeciesKey();
            if (!plantsBySpecies.containsKey(key)) {
                plantsBySpecies.put(key, plant);
                allPlants.add(plant);
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public List<Plant> getPlants() { return allPlants; }
    public Plant getPlant(SpeciesKey key) { return plantsBySpecies.get(key); }

    public int getPlantCount() { 
        double total = 0;
        for (Plant p : allPlants) total += p.getBiomass();
        return (int) total; 
    }

    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            for (int i = allAnimals.size() - 1; i >= 0; i--) {
                Animal a = allAnimals.get(i);
                if (!a.isAlive()) {
                    removeAnimal(a);
                }
            }
        } finally { lock.unlock(); }
    }

    public String getStatistics() {
        return String.format("Cell[%d,%d]: Животные=%d, Растения=%d", x, y, allAnimals.size(), allPlants.size());
    }
}
