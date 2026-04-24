package com.island.model;

import com.island.content.Animal;
import com.island.content.Plant;
import lombok.Getter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Getter
public class Cell {
    private final int x, y;
    private final List<Animal> animals = new CopyOnWriteArrayList<>();
    private final List<Plant> plants = new CopyOnWriteArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();

    public Cell(int x, int y) { this.x = x; this.y = y; }

    public String getCoordinates() { return x + "," + y; }

    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            long count = animals.stream()
                    .filter(a -> a.getSpeciesKey()
                            .equals(animal.getSpeciesKey()))
                    .count();
            if (count >= animal.getMaxPerCell()) return false;
            return animals.add(animal);
        } finally { lock.unlock(); }
    }

    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try { return animals.remove(animal); } finally { lock.unlock(); }
    }

    public List<Animal> getAnimals() { return new CopyOnWriteArrayList<>(animals); }

    public List<Animal> getAnimalsBySpecies(String key) {
        return animals.stream()
                .filter(a -> a.getSpeciesKey()
                        .equals(key))
                .collect(Collectors.toList());
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
            return plants.add(plant);
        } finally { lock.unlock(); }
    }

    public boolean removePlant(Plant plant) {
        lock.lock();
        try { return plants.remove(plant); } finally { lock.unlock(); }
    }

    public List<Plant> getPlants() { return new CopyOnWriteArrayList<>(plants); }

    public int getPlantCount() { return plants.size(); }

    public void cleanupDeadOrganisms() {
        lock.lock();
        try {
            animals.removeIf(a -> !a.isAlive());
            plants.removeIf(p -> !p.isAlive());
        } finally { lock.unlock(); }
    }

    public String getStatistics() {
        return String.format("Cell[%d,%d]: Животные=%d, Растения=%d", x, y, animals.size(), plants.size());
    }
}
