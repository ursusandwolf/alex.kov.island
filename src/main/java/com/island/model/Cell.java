package com.island.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.SizeClass;
import com.island.content.SpeciesKey;
import com.island.engine.Mortal;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;

import static com.island.config.SimulationConstants.SCALE_1M;

@Getter
public class Cell implements SimulationNode {
    private final int x;
    private final int y;
    private final SimulationWorld world;
    @Setter private TerrainType terrainType = TerrainType.MEADOW;
    private final EntityContainer container = new EntityContainer();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private List<SimulationNode> cachedNeighbors = java.util.Collections.emptyList();

    public Cell(int x, int y, SimulationWorld world) { 
        this.x = x; 
        this.y = y; 
        this.world = world;
    }

    @Override
    public Lock getLock() {
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
    public List<? extends Mortal> getLivingEntities() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllAnimals());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<? extends Mortal> getBiomassEntities() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(container.getAllBiomass());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean canAccept(Animal animal) {
        rwLock.readLock().lock();
        try {
            if (!animal.getAnimalType().isTerrainAccessible(terrainType)) {
                return false;
            }
            return container.countByType(animal.getAnimalType()) < animal.getMaxPerCell();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean addEntity(Mortal entity) {
        if (entity instanceof Animal a) {
            return addAnimal(a);
        } else if (entity instanceof Biomass b) {
            return addBiomass(b);
        }
        return false;
    }

    @Override
    public boolean removeEntity(Mortal entity) {
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

    public void forEachAnimal(Consumer<Animal> action) {
        List<Animal> copy;
        rwLock.readLock().lock();
        try {
            copy = new ArrayList<>(container.getAllAnimals());
        } finally {
            rwLock.readLock().unlock();
        }
        copy.forEach(action);
    }

    public void forEachAnimalReadOnly(Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            container.getAllAnimals().forEach(action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachAnimalSampled(int limit, RandomProvider random, Consumer<Animal> action) {
        List<Animal> sampled = new ArrayList<>();
        rwLock.readLock().lock();
        try {
            Set<Animal> set = container.getAllAnimals();
            int size = set.size();
            if (size == 0) {
                return;
            }
            int step = (size > limit) ? (size / limit + 1) : 1;
            int startOffset = (size > limit) ? random.nextInt(step) : 0;
            int i = 0;
            for (Animal a : set) {
                if (i >= startOffset && (i - startOffset) % step == 0) {
                    sampled.add(a);
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        sampled.forEach(action);
    }

    public void forEachPredator(Consumer<Animal> action) {
        List<Animal> copy;
        rwLock.readLock().lock();
        try {
            copy = new ArrayList<>(container.getPredators());
        } finally {
            rwLock.readLock().unlock();
        }
        copy.forEach(action);
    }

    public void forEachHerbivoreSampled(int limit, RandomProvider random, Consumer<Animal> action) {
        List<Animal> sampled = new ArrayList<>();
        rwLock.readLock().lock();
        try {
            Set<Animal> set = container.getHerbivores();
            int size = set.size();
            if (size == 0) {
                return;
            }
            int step = (size > limit) ? (size / limit + 1) : 1;
            int startOffset = (size > limit) ? random.nextInt(step) : 0;
            int i = 0;
            for (Animal a : set) {
                if (i >= startOffset && (i - startOffset) % step == 0) {
                    sampled.add(a);
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        sampled.forEach(action);
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

    public boolean addBiomass(SpeciesKey key, long amount) {
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
            long total = 0;
            for (Biomass b : container.getAllBiomass()) {
                total += b.getBiomass();
            }
            return (int) (total / SCALE_1M);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public String getStatistics() {
        rwLock.readLock().lock();
        try {
            return String.format("Cell[%d,%d]: Animals=%d, Biomass=%d", x, y, container.getAllAnimals().size(), getPlantCount());
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
