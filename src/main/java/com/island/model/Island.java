package com.island.model;

import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.SpeciesConfig;
import com.island.content.plants.Plant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Composite: Island consists of cells.
 */
public class Island {
    private final int width, height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private int tickCount = 0;
    private boolean redBookProtectionEnabled = true;
    
    private final Map<String, AtomicInteger> speciesCounts = new ConcurrentHashMap<>();
    private final Map<com.island.content.DeathCause, Map<String, AtomicInteger>> deathStats = new ConcurrentHashMap<>();
    private final Map<com.island.content.DeathCause, Map<String, AtomicInteger>> totalDeathStats = new ConcurrentHashMap<>();

    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
        for (com.island.content.DeathCause cause : com.island.content.DeathCause.values()) {
            deathStats.put(cause, new ConcurrentHashMap<>());
            totalDeathStats.put(cause, new ConcurrentHashMap<>());
        }
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Cell[][] getGrid() { return grid; }
    public List<Chunk> getChunks() { return chunks; }
    public int getTickCount() { return tickCount; }

    public void setRedBookProtectionEnabled(boolean enabled) {
        this.redBookProtectionEnabled = enabled;
    }

    public boolean isRedBookProtectionEnabled() {
        return redBookProtectionEnabled;
    }

    public Map<String, Double> getProtectionMap(SpeciesConfig config) {
        if (!redBookProtectionEnabled) return Collections.emptyMap();
        
        Map<String, Double> protectionMap = new HashMap<>();
        int islandArea = width * height;

        for (String key : config.getAllSpeciesKeys()) {
            AnimalType type = config.getAnimalType(key);
            if (type == null) continue;

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

    public void reportDeath(String speciesKey, com.island.content.DeathCause cause) {
        deathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
        totalDeathStats.get(cause)
                 .computeIfAbsent(speciesKey, k -> new AtomicInteger(0))
                 .incrementAndGet();
    }

    public int getDeathCount(com.island.content.DeathCause cause) {
        return deathStats.get(cause).values().stream().mapToInt(AtomicInteger::get).sum();
    }

    public int getTotalAnimalDeathCount(com.island.content.DeathCause cause) {
        return totalDeathStats.get(cause).entrySet().stream()
                .filter(e -> !isPlantKey(e.getKey()))
                .mapToInt(e -> e.getValue().get())
                .sum();
    }

    private boolean isPlantKey(String key) {
        return key.equalsIgnoreCase("plant") || 
               key.equalsIgnoreCase("cabbage") || 
               key.equalsIgnoreCase("caterpillar");
    }

    public Map<String, Integer> getDeathsBySpecies(com.island.content.DeathCause cause) {
        Map<String, Integer> result = new HashMap<>();
        deathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public Map<String, Integer> getTotalDeathsBySpecies(com.island.content.DeathCause cause) {
        Map<String, Integer> result = new HashMap<>();
        totalDeathStats.get(cause).forEach((k, v) -> result.put(k, v.get()));
        return result;
    }

    public void nextTick() {
        tickCount++;
        for (Map<String, AtomicInteger> stats : deathStats.values()) {
            stats.clear();
        }
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = new Cell(x, y, this);
    }

    private void partitionIntoChunks() {
        int midX = width / 2;
        int midY = height / 2;

        if (midX > 0 && midY > 0) {
            chunks.add(new Chunk(0, 0, 0, midX, 0, midY, this));
            chunks.add(new Chunk(1, 0, midX, width, 0, midY, this));
            chunks.add(new Chunk(0, 1, 0, midX, midY, height, this));
            chunks.add(new Chunk(1, 1, midX, width, midY, height, this));
        } else if (midX > 0) {
            chunks.add(new Chunk(0, 0, 0, midX, 0, height, this));
            chunks.add(new Chunk(1, 0, midX, width, 0, height, this));
        } else {
            chunks.add(new Chunk(0, 0, 0, width, 0, height, this));
        }
    }

    public Cell getCell(int x, int y) {
        int wx = ((x % width) + width) % width;
        int wy = ((y % height) + height) % height;
        return grid[wx][wy];
    }

    public Cell getCellUnsafe(int x, int y) {
        return (x < 0 || x >= width || y < 0 || y >= height) ? null : grid[x][y];
    }

    public int getSpeciesCount(String speciesKey) {
        AtomicInteger count = speciesCounts.get(speciesKey);
        return (count != null) ? count.get() : 0;
    }

    public void onOrganismAdded(String speciesKey) {
        speciesCounts.computeIfAbsent(speciesKey, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void onOrganismRemoved(String speciesKey) {
        AtomicInteger count = speciesCounts.get(speciesKey);
        if (count != null) count.decrementAndGet();
    }

    public boolean moveOrganism(Animal animal, Cell from, Cell to) {
        if (from == to) return true;

        Cell first = (System.identityHashCode(from) < System.identityHashCode(to)) ? from : to;
        Cell second = (first == from) ? to : from;

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (from.getAnimals().contains(animal)) {
                    if (to.addAnimal(animal)) {
                        from.removeAnimal(animal);
                        return true;
                    }
                }
                return false;
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    public Map<String, Integer> getSpeciesCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (Map.Entry<String, AtomicInteger> entry : speciesCounts.entrySet()) {
            int count = entry.getValue().get();
            if (count > 0) counts.put(entry.getKey(), count);
        }

        double totalPlant = 0;
        double totalCabbage = 0;
        double totalCaterpillar = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Plant p : grid[x][y].getPlants()) {
                    String sKey = p.getSpeciesKey();
                    if (sKey.equals("plant")) totalPlant += p.getBiomass();
                    else if (sKey.equals("cabbage")) totalCabbage += p.getBiomass();
                    else if (sKey.equals("caterpillar")) totalCaterpillar += p.getBiomass();
                }
            }
        }
        if (totalPlant > 0) counts.put("plant", (int) totalPlant);
        if (totalCabbage > 0) counts.put("cabbage", (int) totalCabbage);
        if (totalCaterpillar > 0) counts.put("caterpillar", (int) totalCaterpillar);

        return counts;
    }

    public int getTotalOrganismCount() {
        int animalTotal = speciesCounts.values().stream().mapToInt(AtomicInteger::get).sum();
        double plantTotal = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                plantTotal += grid[x][y].getPlantCount();
            }
        }
        return animalTotal + (int) plantTotal;
    }

    public String getStatistics() {
        return String.format("Остров[%dx%d] Всего организмов: %d", width, height, getTotalOrganismCount());
    }
}
