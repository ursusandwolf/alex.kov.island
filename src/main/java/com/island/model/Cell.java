package com.island.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
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
public class Cell implements SimulationNode {
    private final int x;
    private final int y;
    private final SimulationWorld world;
    private final EntityContainer container = new EntityContainer();
    private final ReentrantLock lock = new ReentrantLock();

    public Cell(int x, int y, SimulationWorld world) { 
        this.x = x; 
        this.y = y; 
        this.world = world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public SimulationWorld getWorld() {
        return world;
    }

    @Override
    public ReentrantLock getLock() {
        return lock;
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    public boolean addAnimal(Animal animal) {
        lock.lock();
        try {
            if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
                return false;
            }
            
            container.addAnimal(animal);
            world.onOrganismAdded(animal.getSpeciesKey());
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeAnimal(Animal animal) {
        lock.lock();
        try { 
            if (container.removeAnimal(animal)) {
                world.onOrganismRemoved(animal.getSpeciesKey());
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getAnimals() {
        lock.lock();
        try {
            return new ArrayList<>(container.getAllAnimals());
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getPredators() {
        lock.lock();
        try {
            return new ArrayList<>(container.getPredators());
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getHerbivores() {
        lock.lock();
        try {
            return new ArrayList<>(container.getHerbivores());
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getAnimalsByType(AnimalType type) {
        lock.lock();
        try {
            return new ArrayList<>(container.getByType(type));
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> getAnimalsBySize(SizeClass size) {
        lock.lock();
        try {
            return new ArrayList<>(container.getBySize(size));
        } finally {
            lock.unlock();
        }
    }

    public int countAnimalsByType(AnimalType type) {
        lock.lock();
        try {
            return container.countByType(type);
        } finally {
            lock.unlock();
        }
    }

    public int getAnimalCount() {
        lock.lock();
        try {
            return container.getAllAnimals().size();
        } finally {
            lock.unlock();
        }
    }

    public boolean addBiomass(Biomass b) {
        lock.lock();
        try {
            container.addBiomass(b);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public boolean addBiomass(SpeciesKey key, double amount) {
        lock.lock();
        try {
            Biomass existing = container.getBiomass(key);
            if (existing != null) {
                existing.addBiomass(amount);
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean removeBiomass(Biomass b) {
        lock.lock();
        try {
            return container.removeBiomass(b);
        } finally {
            lock.unlock();
        }
    }

    public List<Biomass> getBiomassContainers() {
        lock.lock();
        try {
            return new ArrayList<>(container.getAllBiomass());
        } finally {
            lock.unlock();
        }
    }

    public Biomass getBiomass(SpeciesKey key) {
        lock.lock();
        try {
            return container.getBiomass(key);
        } finally {
            lock.unlock();
        }
    }

    public int getPlantCount() { 
        lock.lock();
        try {
            double total = 0;
            for (Biomass b : container.getAllBiomass()) {
                total += b.getBiomass();
            }
            return (int) total;
        } finally {
            lock.unlock();
        }
    }

    public List<Animal> cleanupDeadOrganisms() {
        lock.lock();
        try {
            List<Animal> toRemove = new ArrayList<>();
            for (Animal a : container.getAllAnimals()) {
                if (!a.isAlive()) {
                    toRemove.add(a);
                }
            }
            for (Animal a : toRemove) {
                removeAnimal(a);
            }
            return toRemove;
        } finally {
            lock.unlock();
        }
    }

    public String getStatistics() {
        lock.lock();
        try {
            return String.format("Cell[%d,%d]: Animals=%d, Biomass=%d", x, y, container.getAllAnimals().size(), (int) getPlantCount());
        } finally {
            lock.unlock();
        }
    }
}
