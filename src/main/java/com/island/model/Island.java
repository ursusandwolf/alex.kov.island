package com.island.model;
import com.island.util.RandomUtils;
import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.SpeciesConfig;
import com.island.content.plants.Plant;
import lombok.Getter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


// Composite: Остров состоит из ячеек
@Getter
public class Island {
    private final int width, height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private int tickCount = 0;
    private boolean redBookProtectionEnabled = true;
    
    private final Map<String, AtomicInteger> speciesCounts = new ConcurrentHashMap<>();

    public Island(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
    }

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
            
            double threshold = globalCapacity * 0.05; // Lower to 5%
            if (currentCount > 0 && currentCount < threshold) {
                double ratio = (double) currentCount / threshold;
                // Probability: 30% to 60%
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

    public void nextTick() {
        tickCount++;
    }

    public int getTickCount() {
        return tickCount;
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                grid[x][y] = new Cell(x, y, this);
    }

    private void partitionIntoChunks() {
        // Split island into 4 chunks (2x2 grid)
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

    public void initializeWorld(SpeciesConfig config) {
        // TODO: Инициализация мира
    }

    public Map<String, Integer> getSpeciesCounts() {
        Map<String, Integer> counts = new HashMap<>();
        
        // Add animal counts
        for (Map.Entry<String, AtomicInteger> entry : speciesCounts.entrySet()) {
            int count = entry.getValue().get();
            if (count > 0) counts.put(entry.getKey(), count);
        }

        // Add plant biomass units from all cells
        double totalPlant = 0;
        double totalCabbage = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (Plant p : grid[x][y].getPlants()) {
                    if (p.getSpeciesKey().equals("plant")) totalPlant += p.getBiomass();
                    if (p.getSpeciesKey().equals("cabbage")) totalCabbage += p.getBiomass();
                }
            }
        }
        if (totalPlant > 0) counts.put("plant", (int) totalPlant);
        if (totalCabbage > 0) counts.put("cabbage", (int) totalCabbage);

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
