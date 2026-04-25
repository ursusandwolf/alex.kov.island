package com.island.model;
import com.island.content.plants.*;
import com.island.content.Animal;
import com.island.content.plants.Plant;
import lombok.Getter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class Cell {
    private final int x, y;
    private final Island island;
    private final List<Animal> animals = new CopyOnWriteArrayList<>();
    private final List<Plant> plants = new CopyOnWriteArrayList<>();
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
            long count = animals.stream()
                    .filter(a -> a.getSpeciesKey()
                            .equals(animal.getSpeciesKey()))
                    .count();
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

    public List<Animal> getAnimals() { return Collections.unmodifiableList(animals); }

    public List<Animal> getAnimalsBySpecies(String key) {
        return animals.stream()
                .filter(a -> a.getSpeciesKey()
                        .equals(key))
                .toList();
    }

    public int countAnimalsBySpecies(String key) {
        return (int) animals.stream()
                .filter(a -> a.getSpeciesKey()
                        .equals(key))
                .count();
    }

    public int getAnimalCount() { return animals.size(); }

    public boolean addPlant(Plant plant) {
        lock.lock();
        try {
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

    public List<Plant> getPlants() { return Collections.unmodifiableList(plants); }

    public int getPlantCount() { return plants.size(); }

    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            animals.removeIf(a -> {
                if (!a.isAlive()) {
                    island.onOrganismRemoved(a.getSpeciesKey());
                    return true;
                }
                return false;
            });
            plants.removeIf(p -> {
                if (!p.isAlive()) {
                    island.onOrganismRemoved(p.getSpeciesKey());
                    return true;
                }
                return false;
            });
        } finally { lock.unlock(); }
    }

    public String getStatistics() {
        return String.format("Cell[%d,%d]: Животные=%d, Растения=%d", x, y, animals.size(), plants.size());
    }
}
