package com.island.model;
import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.plants.Plant;
import lombok.Getter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Cell {
    private final int x, y;
    private final Island island;
    private final List<Animal> animals = new ArrayList<>();
    private final List<Plant> plants = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Cell(int x, int y, Island island) { 
        this.x = x; 
        this.y = y; 
        this.island = island;
    }

    public String getCoordinates() { return x + "," + y; }

    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            int count = 0;
            String key = animal.getSpeciesKey();
            for (Animal a : animals) {
                if (a.getSpeciesKey().equals(key)) count++;
            }
            if (count >= animal.getMaxPerCell()) return false;
            
            if (animals.add(animal)) {
                island.onOrganismAdded(animal.getSpeciesKey());
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try { 
            if (animals.remove(animal)) {
                island.onOrganismRemoved(animal.getSpeciesKey());
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public List<Animal> getAnimals() { 
        // Returning a direct reference is dangerous but fast. 
        // For performance, we use a synchronized copy ONLY when needed by services.
        return animals; 
    }

    public List<Animal> getAnimalsBySpecies(String key) {
        lock.lock();
        try {
            List<Animal> result = new ArrayList<>();
            for (Animal a : animals) {
                if (a.getSpeciesKey().equals(key)) result.add(a);
            }
            return result;
        } finally { lock.unlock(); }
    }

    public int countAnimalsBySpecies(String key) {
        lock.lock();
        try {
            int count = 0;
            for (Animal a : animals) {
                if (a.getSpeciesKey().equals(key)) count++;
            }
            return count;
        } finally { lock.unlock(); }
    }

    public int getAnimalCount() { 
        return animals.size(); 
    }

    public boolean addPlant(Plant plant) {
        lock.lock();
        try {
            if (plants.size() >= 200) return false;
            if (plants.add(plant)) {
                island.onOrganismAdded(plant.getSpeciesKey());
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public boolean removePlant(Plant plant) {
        lock.lock();
        try { 
            if (plants.remove(plant)) {
                island.onOrganismRemoved(plant.getSpeciesKey());
                return true;
            }
            return false;
        } finally { lock.unlock(); }
    }

    public List<Plant> getPlants() { return plants; }

    public int getPlantCount() { return plants.size(); }

    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            for (int i = animals.size() - 1; i >= 0; i--) {
                Animal a = animals.get(i);
                if (!a.isAlive()) {
                    island.onOrganismRemoved(a.getSpeciesKey());
                    animals.remove(i);
                }
            }
            for (int i = plants.size() - 1; i >= 0; i--) {
                Plant p = plants.get(i);
                if (!p.isAlive()) {
                    island.onOrganismRemoved(p.getSpeciesKey());
                    plants.remove(i);
                }
            }
        } finally { lock.unlock(); }
    }

    public String getStatistics() {
        return String.format("Cell[%d,%d]: Животные=%d, Растения=%d", x, y, animals.size(), plants.size());
    }
}
