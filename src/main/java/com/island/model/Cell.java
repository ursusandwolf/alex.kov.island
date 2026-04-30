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
    private final java.util.concurrent.locks.ReadWriteLock rwLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
    private List<SimulationNode> cachedNeighbors = java.util.Collections.emptyList();

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
    public java.util.concurrent.locks.Lock getLock() {
        return rwLock.writeLock();
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    @Override
    public void setNeighbors(List<SimulationNode> neighbors) {
        rwLock.writeLock().lock();
        try {
            this.cachedNeighbors = List.copyOf(neighbors);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public List<SimulationNode> getNeighbors() {
        rwLock.readLock().lock();
        try {
            return cachedNeighbors;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<? extends com.island.engine.Mortal> getLivingEntities() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllAnimals());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<? extends com.island.engine.Mortal> getBiomassEntities() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllBiomass());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean addEntity(com.island.engine.Mortal entity) {
        if (entity instanceof Animal a) {
            return addAnimal(a);
        } else if (entity instanceof Biomass b) {
            return addBiomass(b);
        }
        return false;
    }

    @Override
    public boolean removeEntity(com.island.engine.Mortal entity) {
        if (entity instanceof Animal a) {
            return removeAnimal(a);
        } else if (entity instanceof Biomass b) {
            return removeBiomass(b);
        }
        return false;
    }

    public boolean addAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try {
            if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
                return false;
            }
            
            container.addAnimal(animal);
            world.onOrganismAdded(animal.getSpeciesKey());
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean removeAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try { 
            if (container.removeAnimal(animal)) {
                world.onOrganismRemoved(animal.getSpeciesKey());
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<Animal> getAnimals() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllAnimals());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Executes an action for each animal in the cell under a read lock.
     * Prevents defensive copying and reduces GC pressure.
     */
    public void forEachAnimal(java.util.function.Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            for (Animal a : container.getAllAnimals()) {
                action.accept(a);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    /**
     * Executes an action for a sampled subset of animals to maintain LOD without copying.
     */
    public void forEachAnimalSampled(int limit, com.island.util.RandomProvider random, java.util.function.Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            java.util.Set<Animal> set = container.getAllAnimals();
            int size = set.size();
            if (size == 0) {
                return;
            }
            int step = (size > limit) ? (size / limit + 1) : 1;
            int startOffset = (size > limit) ? random.nextInt(step) : 0;
            
            int i = 0;
            for (Animal a : set) {
                if (i >= startOffset && (i - startOffset) % step == 0) {
                    action.accept(a);
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachPredator(java.util.function.Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            for (Animal a : container.getPredators()) {
                action.accept(a);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachHerbivoreSampled(int limit, com.island.util.RandomProvider random, java.util.function.Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            java.util.Set<Animal> set = container.getHerbivores();
            int size = set.size();
            if (size == 0) {
                return;
            }
            int step = (size > limit) ? (size / limit + 1) : 1;
            int startOffset = (size > limit) ? random.nextInt(step) : 0;
            
            int i = 0;
            for (Animal a : set) {
                if (i >= startOffset && (i - startOffset) % step == 0) {
                    action.accept(a);
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> getPredators() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getPredators());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> getHerbivores() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getHerbivores());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> getAnimalsByType(AnimalType type) {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getByType(type));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> getAnimalsBySize(SizeClass size) {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getBySize(size));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int countAnimalsByType(AnimalType type) {
        rwLock.readLock().lock();
        try {
            return container.countByType(type);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getOrganismCount(SpeciesKey key) {
        rwLock.readLock().lock();
        try {
            return container.countBySpecies(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getAnimalCount() {
        rwLock.readLock().lock();
        try {
            return container.getAllAnimals().size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean addBiomass(Biomass b) {
        rwLock.writeLock().lock();
        try {
            Biomass existing = container.getBiomass(b.getSpeciesKey());
            if (existing != null) {
                existing.addBiomass(b.getBiomass(), this);
                return true;
            }
            container.addBiomass(b);
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean addBiomass(SpeciesKey key, double amount) {
        rwLock.writeLock().lock();
        try {
            Biomass existing = container.getBiomass(key);
            if (existing != null) {
                existing.addBiomass(amount, this);
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean removeBiomass(Biomass b) {
        rwLock.writeLock().lock();
        try {
            return container.removeBiomass(b);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public List<Biomass> getBiomassContainers() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllBiomass());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Biomass getBiomass(SpeciesKey key) {
        rwLock.readLock().lock();
        try {
            return container.getBiomass(key);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getPlantCount() { 
        rwLock.readLock().lock();
        try {
            double total = 0;
            for (Biomass b : container.getAllBiomass()) {
                total += b.getBiomass();
            }
            return (int) total;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> cleanupDeadOrganisms() {
        rwLock.writeLock().lock();
        try {
            List<Animal> toRemove = new ArrayList<>();
            for (Animal a : container.getAllAnimals()) {
                if (!a.isAlive()) {
                    toRemove.add(a);
                }
            }
            for (Animal a : toRemove) {
                container.removeAnimal(a);
                world.onOrganismRemoved(a.getSpeciesKey());
            }
            return toRemove;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public String getStatistics() {
        rwLock.readLock().lock();
        try {
            return String.format("Cell[%d,%d]: Animals=%d, Biomass=%d", x, y, container.getAllAnimals().size(), (int) getPlantCount());
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
