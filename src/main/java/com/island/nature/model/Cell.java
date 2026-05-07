package com.island.nature.model;

import com.island.engine.ecs.EntityQuery;
import com.island.nature.config.Configuration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
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

    /**
     * Type-safe version of getNeighbors for the nature domain.
     */
    @SuppressWarnings("unchecked")
    public List<Cell> getCellNeighbors() {
        rwLock.readLock().lock();
        try {
            // Safe as long as we only put Cells into Island grid
            return (List<Cell>) (List<?>) cachedNeighbors;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Organism> getEntities() {
        rwLock.readLock().lock();
        try {
            List<Organism> all = new ArrayList<>(getEntityCount());
            container.forEachEntity(all::add);
            return Collections.unmodifiableList(all);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void forEachEntity(Consumer<Organism> action) {
        rwLock.readLock().lock();
        try {
            container.forEachEntity(action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void query(EntityQuery<Organism> query, Consumer<Organism> action) {
        rwLock.readLock().lock();
        try {
            container.forEachMatching(query, action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int getEntityCount() {
        rwLock.readLock().lock();
        try {
            return container.getEntityCount();
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
        return addAnimal(animal, true);
    }

    public boolean addAnimal(Animal animal, boolean fireEvents) {
        rwLock.writeLock().lock();
        try {
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
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public boolean removeAnimal(Animal animal) {
        rwLock.writeLock().lock();
        try { 
            if (container.removeAnimal(animal)) {
                world.onEntityRemoved(animal);
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
            List<Animal> predators = new ArrayList<>();
            container.forEachAnimal(a -> {
                if (a.isAnimalPredator()) {
                    predators.add(a);
                }
            });
            return predators;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public List<Animal> getHerbivores() {
        rwLock.readLock().lock();
        try {
            List<Animal> herbivores = new ArrayList<>();
            container.forEachAnimal(a -> {
                if (!a.isAnimalPredator()) {
                    herbivores.add(a);
                }
            });
            return herbivores;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachAnimal(Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            container.forEachAnimal(action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachAnimalSampled(SamplingContext context, Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            List<Animal> all = new ArrayList<>(getAnimalCount());
            container.forEachAnimal(all::add);
            SamplingUtils.forEachSampled(all, context, action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachPredator(Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            container.forEachAnimal(a -> {
                if (a.isAnimalPredator()) {
                    action.accept(a);
                }
            });
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void forEachHerbivoreSampled(int limit, RandomProvider random, Consumer<Animal> action) {
        rwLock.readLock().lock();
        try {
            List<Animal> herbivores = new ArrayList<>();
            container.forEachAnimal(a -> {
                if (!a.isAnimalPredator()) {
                    herbivores.add(a);
                }
            });
            SamplingUtils.forEachSampled(herbivores, limit, random, action);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public Animal getRandomAnimalByType(AnimalType type, RandomProvider random) {
        rwLock.readLock().lock();
        try {
            List<Animal> list = container.getByType(type);
            if (list.isEmpty()) {
                return null;
            }
            return list.get(random.nextInt(list.size()));
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
            return container.getAnimalCount();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public int getBiomassCount() {
        rwLock.readLock().lock();
        try {
            return container.getBiomassCount();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean addBiomass(Biomass b) {
        rwLock.writeLock().lock();
        try {
            Biomass existing = container.getBiomass(b.getSpeciesKey());
            if (existing != null) {
                existing.addBiomassAmount(b.getBiomass(), this);
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
                existing.addBiomassAmount(amount, this);
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
            return (int) (total / config.getScale1M());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void cleanupDeadEntities(Consumer<Organism> onOrganismRemoved) {
        rwLock.writeLock().lock();
        try {
            container.removeDeadAnimals(a -> {
                world.onEntityRemoved(a);
                onOrganismRemoved.accept((Organism) a);
            });
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}