package com.island.model;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Composite: Island consists of cells.
 */
public class Island {
    private final int width;
    private final int height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private int tickCount = 0;
    private boolean redBookProtectionEnabled = true;
    
    private final Map<SpeciesKey, AtomicInteger> speciesCounts = new EnumMap<>(SpeciesKey.class);
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> deathStats = new EnumMap<>(DeathCause.class);
    private final Map<DeathCause, Map<SpeciesKey, AtomicInteger>> totalDeathStats = new EnumMap<>(DeathCause.class);

    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
        for (DeathCause cause : DeathCause.values()) {
            deathStats.put(cause, new ConcurrentHashMap<>());
            totalDeathStats.put(cause, new ConcurrentHashMap<>());
        }
        for (SpeciesKey key : SpeciesKey.values()) {
            speciesCounts.put(key, new AtomicInteger(0));
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Cell[][] getGrid() {
        return grid;
    }

    public List<Chunk> getChunks() {
        return chunks;
    }

    public int getTickCount() {
        return tickCount;
    }

    public void setRedBookProtectionEnabled(boolean enabled) {
        this.redBookProtectionEnabled = enabled;
    }

    public boolean isRedBookProtectionEnabled() {
        return redBookProtectionEnabled;
    }

    public Map<SpeciesKey, Double> getProtectionMap(SpeciesRegistry registry) {
        if (!redBookProtectionEnabled) {
            return Collections.emptyMap();
        }
        
        Map<SpeciesKey, Double> protectionMap = new EnumMap<>(SpeciesKey.class);
        int islandArea = width * height;

        for (SpeciesKey key : registry.getAllAnimalKeys()) {
            AnimalType type = registry.getAnimalType(key).orElse(null);
            if (type == null) {
                continue;
            }

            int currentCount = getSpeciesCount(key);
            int globalCapacity = islandArea * type.getMaxPerCell();
            
            double threshold = globalCapacity * 0.05; 
            if (currentCount > 0 && currentCount < threshold) {
                double ratio = (double) currentCount / threshold;
                double hideChance = 0.60 - (ratio * 0.30);
                protectionMap.put(key, hideChance);
            }
        }
        return protectionMap;
    }

    public double getGlobalSatiety() {
        double totalMax = 0;
        double totalCurrent = 0;
        int animalCount = 0;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Animal a : grid[x][y].getAnimals()) {
                    if (a.isAlive()) {
                        totalMax += a.getMaxEnergy();
                        totalCurrent += a.getCurrentEnergy();
                        animalCount++;
                    }
                }
            }
        }
        return (animalCount == 0) ? 100.0 : (totalCurrent / totalMax) * 100.0;
    }

    public int getStarvingCount() {
        int starving = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Animal a : grid[x][y].getAnimals()) {
                    if (a.isAlive() && a.getEnergyPercentage() < 30.0) {
                        starving++;
                    }
                }
            }
        }
        return starving;
    }

    public void reportDeath(SpeciesKey speciesKey, DeathCause cause) {
        deathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
        totalDeathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
    }

    public int getDeathCount(DeathCause cause) {
        return deathStats.get(cause).values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getTotalAnimalDeathCount(DeathCause cause) {
        return totalDeathStats.get(cause).entrySet().stream()
                .filter(e -> !e.getKey().isBiomass())
                .mapToInt(e -> e.getValue().get())
                .sum();
    }

    public Map<SpeciesKey, Integer> getDeathsBySpecies(DeathCause cause) {
        Map<SpeciesKey, Integer> result = new EnumMap<>(SpeciesKey.class);
        deathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public Map<SpeciesKey, Integer> getTotalDeathsBySpecies(DeathCause cause) {
        Map<SpeciesKey, Integer> result = new EnumMap<>(SpeciesKey.class);
        totalDeathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public void nextTick() {
        tickCount++;
        for (Map<SpeciesKey, AtomicInteger> stats : deathStats.values()) {
            stats.clear();
        }
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new Cell(x, y, this);
            }
        }
    }

    private void partitionIntoChunks() {
        int chunkSize = 20; 
        for (int x = 0; x < width; x += chunkSize) {
            for (int y = 0; y < height; y += chunkSize) {
                chunks.add(new Chunk(x / chunkSize, y / chunkSize, 
                        x, Math.min(x + chunkSize, width), 
                        y, Math.min(y + chunkSize, height), this));
            }
        }
    }

    public Cell getCell(int x, int y) {
        // Toroidal island logic
        int tx = (x % width + width) % width;
        int ty = (y % height + height) % height;
        return grid[tx][ty];
    }

    public void onOrganismAdded(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        if (count != null) {
            count.incrementAndGet();
        }
    }

    public void onOrganismRemoved(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    public int getSpeciesCount(SpeciesKey key) {
        AtomicInteger count = speciesCounts.get(key);
        return (count != null) ? count.get() : 0;
    }

    /**
     * Transitional.
     */
    public int getSpeciesCount(String key) {
        try {
            return getSpeciesCount(SpeciesKey.fromCode(key));
        } catch (IllegalArgumentException e) {
            return 0;
        }
    }

    public Map<SpeciesKey, Integer> getSpeciesCounts() {
        Map<SpeciesKey, Integer> counts = new EnumMap<>(SpeciesKey.class);
        
        // Add Animals (from tracked counts)
        speciesCounts.forEach((k, v) -> {
            if (v.get() > 0) {
                counts.put(k, v.get());
            }
        });

        // Add Biomass (Plants, Caterpillars) - sum their total mass across the island
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Biomass b : grid[x][y].getBiomassContainers()) {
                    if (b.isAlive() && b.getBiomass() > 0) {
                        counts.merge(b.getSpeciesKey(), (int) b.getBiomass(), Integer::sum);
                    }
                }
            }
        }
        
        return counts;
    }

    public int getTotalOrganismCount() {
        int animalTotal = 0;
        for (AtomicInteger count : speciesCounts.values()) {
            animalTotal += count.get();
        }
        
        int biomassTotal = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Biomass b : grid[x][y].getBiomassContainers()) {
                    biomassTotal += (int) b.getBiomass();
                }
            }
        }
        return animalTotal + biomassTotal;
    }

    public void moveOrganism(Animal animal, Cell from, Cell to) {
        if (from == to) {
            return;
        }
        
        Cell first = (from.getX() < to.getX() || (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
        Cell second = (first == from) ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (from.removeAnimal(animal)) {
                    if (!to.addAnimal(animal)) {
                        if (!from.addAnimal(animal)) {
                            animal.tryConsumeEnergy(animal.getCurrentEnergy()); 
                            reportDeath(animal.getSpeciesKey(), DeathCause.MOVEMENT_EXHAUSTION);
                        }
                    }
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    public void moveBiomass(Biomass b, Cell from, Cell to) {
        moveBiomassPartially(b, from, to, b.getBiomass());
    }

    public void moveBiomassPartially(Biomass b, Cell from, Cell to, double amount) {
        if (from == to || amount <= 0 || b.getBiomass() <= 0) {
            return;
        }

        Cell first = (from.getX() < to.getX() || (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
        Cell second = (first == from) ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                // In this optimized model, we can just transfer the value.
                double actualToMove = Math.min(b.getBiomass(), amount);
                
                if (to.addBiomass(b.getSpeciesKey(), actualToMove)) {
                    b.consumeBiomass(actualToMove);
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    public String getStatistics() {
        return String.format("Остров[%dx%d] Всего организмов: %d", width, height, getTotalOrganismCount());
    }
}
