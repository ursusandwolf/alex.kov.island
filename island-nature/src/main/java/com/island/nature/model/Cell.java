package com.island.nature.model;

import com.island.engine.ecs.EntityQuery;
import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SizeClass;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureWorld;
import com.island.util.common.RandomProvider;
import com.island.util.sampling.SamplingContext;
import com.island.util.sampling.SamplingUtils;

@Getter
public class Cell implements SimulationNode<Organism> {
    private final int x;
    private final int y;
    private final SimulationWorld<Organism> world;
    private final Configuration config;
    @Setter private TerrainType terrainType = TerrainType.MEADOW;
    private final EntityContainer container;
    private final StampedLock lock = new StampedLock();
    private List<SimulationNode<Organism>> cachedNeighbors = Collections.emptyList();

    public Cell(int x, int y, SimulationWorld<Organism> world) {
        this.x = x;
        this.y = y;
        this.world = world;
        this.config = ((NatureWorld) world).getConfiguration();
        this.container = new EntityContainer(config);
    }

    @Override
    public SimulationWorld<Organism> getWorld() {
        return world;
    }

    @Override
    public Lock getLock() {
        return lock.asWriteLock();
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    @Override
    public void setNeighbors(List<SimulationNode<Organism>> neighbors) {
        long stamp = lock.writeLock();
        try {
            this.cachedNeighbors = List.copyOf(neighbors);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public List<SimulationNode<Organism>> getNeighbors() {
        long stamp = lock.tryOptimisticRead();
        List<SimulationNode<Organism>> neighbors = cachedNeighbors;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                neighbors = cachedNeighbors;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return neighbors;
    }

    /**
     * Type-safe version of getNeighbors for the nature domain.
     */
    @SuppressWarnings("unchecked")
    public List<Cell> getCellNeighbors() {
        long stamp = lock.tryOptimisticRead();
        List<Cell> neighbors = (List<Cell>) (List<?>) cachedNeighbors;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                neighbors = (List<Cell>) (List<?>) cachedNeighbors;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return neighbors;
    }

    @Override
    public List<Organism> getEntities() {
        long stamp = lock.readLock();
        try {
            List<Organism> all = new ArrayList<>(getEntityCountInternal());
            container.forEachEntity(all::add);
            return Collections.unmodifiableList(all);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void forEachEntity(Consumer<Organism> action) {
        long stamp = lock.readLock();
        try {
            container.forEachEntity(action);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void query(EntityQuery<Organism> query, Consumer<Organism> action) {
        List<Organism> matching = new ArrayList<>();
        long stamp = lock.readLock();
        try {
            container.forEachMatching(query, matching::add);
        } finally {
            lock.unlockRead(stamp);
        }
        matching.forEach(action);
    }

    @Override
    public int getEntityCount() {
        long stamp = lock.tryOptimisticRead();
        int count = container.getEntityCount();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                count = container.getEntityCount();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return count;
    }

    private int getEntityCountInternal() {
        return container.getEntityCount();
    }

    public boolean canAcceptInternal(Organism organism) {
        if (organism instanceof Animal animal) {
            if (!animal.getAnimalType().isTerrainAccessible(terrainType)) {
                return false;
            }
            return container.countByType(animal.getAnimalType()) < animal.getMaxPerCell();
        }
        return true; 
    }

    @Override
    public boolean canAccept(Organism organism) {
        long stamp = lock.readLock();
        try {
            return canAcceptInternal(organism);
        } finally {
            lock.unlockRead(stamp);
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
        return addAnimal(animal, true);
    }

    public boolean addAnimalInternal(Animal animal, boolean fireEvents) {
        if (container.countByType(animal.getAnimalType()) >= animal.getMaxPerCell()) {
            return false;
        }
        container.addAnimal(animal);
        if (fireEvents) {
            world.onEntityAdded(animal);
        } else {
            ((NatureWorld) world).getStatisticsService().registerBirth(animal.getSpeciesKey());
        }
        return true;
    }

    public boolean addAnimal(Animal animal, boolean fireEvents) {
        long stamp = lock.writeLock();
        try {
            return addAnimalInternal(animal, fireEvents);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public boolean removeAnimalInternal(Animal animal) {
        if (container.removeAnimal(animal)) {
            world.onEntityRemoved(animal);
            return true;
        }
        return false;
    }

    public boolean removeAnimal(Animal animal) {
        long stamp = lock.writeLock();
        try { 
            return removeAnimalInternal(animal);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public List<Animal> getAnimals() {
        long stamp = lock.readLock();
        try {
            return new ArrayList<>(container.getAllAnimals());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public List<Animal> getPredators() {
        long stamp = lock.readLock();
        try {
            List<Animal> predators = new ArrayList<>();
            container.forEachAnimal(a -> {
                if (a.isAnimalPredator()) {
                    predators.add(a);
                }
            });
            return predators;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public List<Animal> getHerbivores() {
        long stamp = lock.readLock();
        try {
            List<Animal> herbivores = new ArrayList<>();
            container.forEachAnimal(a -> {
                if (!a.isAnimalPredator()) {
                    herbivores.add(a);
                }
            });
            return herbivores;
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void forEachAnimal(Consumer<Animal> action) {
        long stamp = lock.readLock();
        try {
            container.forEachAnimal(action);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void forEachAnimalSampled(SamplingContext context, Consumer<Animal> action) {
        long stamp = lock.readLock();
        try {
            container.forEachAnimalSampled(context, action);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void forEachPredator(Consumer<Animal> action) {
        long stamp = lock.readLock();
        try {
            container.forEachAnimal(a -> {
                if (a.isAnimalPredator()) {
                    action.accept(a);
                }
            });
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public void forEachHerbivoreSampled(int limit, RandomProvider random, Consumer<Animal> action) {
        long stamp = lock.readLock();
        try {
            container.forEachHerbivoreSampled(limit, random, action);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public Animal getRandomAnimalByType(AnimalType type, RandomProvider random) {
        long stamp = lock.readLock();
        try {
            List<Animal> list = container.getByType(type);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(random.nextInt(list.size()));
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public int countAnimalsByType(AnimalType type) {
        long stamp = lock.tryOptimisticRead();
        int count = container.countByType(type);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                count = container.countByType(type);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return count;
    }

    public int getOrganismCount(SpeciesKey key) {
        long stamp = lock.tryOptimisticRead();
        int count = container.countBySpecies(key);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                count = container.countBySpecies(key);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return count;
    }

    public int getAnimalCount() {
        long stamp = lock.tryOptimisticRead();
        int count = container.getAnimalCount();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                count = container.getAnimalCount();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return count;
    }

    public int getBiomassCount() {
        long stamp = lock.tryOptimisticRead();
        int count = container.getBiomassCount();
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                count = container.getBiomassCount();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return count;
    }

    public boolean addBiomass(Biomass b) {
        long stamp = lock.writeLock();
        try {
            Biomass existing = container.getBiomass(b.getSpeciesKey());
            if (existing != null) {
                existing.addBiomassAmount(b.getBiomass(), this);
                return true;
            }
            container.addBiomass(b);
            return true;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public boolean addBiomass(SpeciesKey key, long amount) {
        long stamp = lock.writeLock();
        try {
            Biomass existing = container.getBiomass(key);
            if (existing != null) {
                existing.addBiomassAmount(amount, this);
                return true;
            }
            return false;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public List<Biomass> getBiomassContainers() {
        long stamp = lock.readLock();
        try {
            return new ArrayList<>(container.getAllBiomass());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public Biomass getBiomass(SpeciesKey key) {
        long stamp = lock.readLock();
        try {
            return container.getBiomass(key);
        } finally {
            lock.unlockRead(stamp);
        }
    }

    public int getPlantCount() { 
        long stamp = lock.readLock();
        try {
            long total = 0;
            for (Biomass b : container.getAllBiomass()) {
                total += b.getBiomass();
            }
            return (int) (total / config.getScale1M());
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void cleanupDeadEntities(Consumer<Organism> onOrganismRemoved) {
        long stamp = lock.writeLock();
        try {
            container.removeDeadAnimals(a -> {
                world.onEntityRemoved(a);
                onOrganismRemoved.accept((Organism) a);
            });
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}