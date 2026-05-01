package com.island.nature.model;

import static com.island.nature.config.SimulationConstants.SCALE_1M;

import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.Organism;
import com.island.nature.entities.SizeClass;
import com.island.nature.entities.SpeciesKey;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class Cell implements SimulationNode<Organism> {
    private final int x;
    private final int y;
    private final SimulationWorld<Organism> world;
    @Setter private TerrainType terrainType = TerrainType.MEADOW;
    private final EntityContainer container = new EntityContainer();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private List<SimulationNode<Organism>> cachedNeighbors = Collections.emptyList();

    @Override
    public Lock getLock() {
        return rwLock.writeLock();
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    @Override
    public void setNeighbors(List<SimulationNode<Organism>> neighbors) {
        rwLock.writeLock().lock();
        try {
            this.cachedNeighbors = List.copyOf(neighbors);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public List<SimulationNode<Organism>> getNeighbors() {
        rwLock.readLock().lock();
        try {
            return cachedNeighbors;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Organism> getEntities() {
        rwLock.readLock().lock();
        try {
            List<Organism> all = new ArrayList<>();
            all.addAll(container.getAllAnimals());
            all.addAll(container.getAllBiomass());
            return all;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void forEachEntity(Consumer<Organism> action) {
        rwLock.readLock().lock();
        try {
            container.getAllAnimals().forEach(action);
            container.getAllBiomass().forEach(action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getEntityCount() {
        rwLock.readLock().lock();
        try {
            return container.getAllAnimals().size() + container.getAllBiomass().size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean canAccept(Organism organism) {
        rwLock.readLock().lock();
        try {
            if (organism instanceof Animal animal) {
                if (!animal.getAnimalType().isTerrainAccessible(terrainType)) {
                    return false;
                }
                return container.countByType(animal.getAnimalType()) < animal.getMaxPerCell();
            }
            return true; 
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean addEntity(Organism entity) {
        return switch (entity) {
            case Animal a -> addAnimal(a);
            case Biomass b -> addBiomass(b);
            default -> false;
        };
    }

    @Override
    public boolean removeEntity(Organism entity) {
        return switch (entity) {
            case Animal a -> removeAnimal(a);
            default -> false;
        };
    }

    public boolean addAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try {
            if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
                return false;
            }
            container.addAnimal(animal);
            if (world instanceof Island island) {
                island.onOrganismAdded(animal.getSpeciesKey());
            }
            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean removeAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try { 
            if (container.removeAnimal(animal)) {
                if (world instanceof Island island) {
                    island.onOrganismRemoved(animal.getSpeciesKey());
                }
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

    public void forEachAnimal(Consumer<Animal> action) {
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
                    if (sampled.size() >= limit) {
                        break;
                    }
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        sampled.forEach(action);
    }

    public void forEachPredator(Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            container.getPredators().forEach(action);
        } finally {
            rwLock.readLock().unlock();
        }
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
                    if (sampled.size() >= limit) {
                        break;
                    }
                }
                i++;
            }
        } finally {
            rwLock.readLock().unlock();
        }
        sampled.forEach(action);
    }

    public Animal getRandomAnimalByType(AnimalType type, RandomProvider random) {
        rwLock.readLock().lock();
        try {
            Set<Animal> set = container.getByType(type);
            if (set.isEmpty()) {
                return null;
            }
            int size = set.size();
            int index = random.nextInt(size);
            int i = 0;
            for (Animal a : set) {
                if (i == index) {
                    return a;
                }
                i++;
            }
            return null;
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

    public int getBiomassCount() {
        rwLock.readLock().lock();
        try {
            return container.getAllBiomass().size();
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

    @Override
    public void cleanupDeadEntities(Consumer<Organism> onOrganismRemoved) {
        rwLock.writeLock().lock();
        try {
            container.removeDeadAnimals(a -> onOrganismRemoved.accept((Organism) a));
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
