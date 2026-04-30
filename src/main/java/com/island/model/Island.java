package com.island.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.content.Animal;
import com.island.content.AnimalType;
import com.island.content.Biomass;
import com.island.content.DeathCause;
import com.island.content.SpeciesRegistry;
import com.island.content.SpeciesKey;
import com.island.service.StatisticsService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;

/**
 * Composite: Island consists of cells.
 */
public class Island implements SimulationWorld {
    @Getter
    private final int width;
    @Getter
    private final int height;
    @Getter
    private final Cell[][] grid;
    @Getter
    private final List<Chunk> chunks = new ArrayList<>();
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    @Getter
    private int tickCount = 0;
    @Getter
    private boolean redBookProtectionEnabled = true;

    public Island(int width, int height, SpeciesRegistry registry, StatisticsService statisticsService) {
        this.width = width;
        this.height = height;
        this.registry = registry;
        this.statisticsService = statisticsService;
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
    }

    public void init() {
        initNeighbors();
    }

    private void initNeighbors() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = grid[x][y];
                List<SimulationNode> neighbors = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        getNode(cell, dx, dy).ifPresent(neighbors::add);
                    }
                }
                cell.setNeighbors(neighbors);
            }
        }
    }

    public void setRedBookProtectionEnabled(boolean enabled) {
        this.redBookProtectionEnabled = enabled;
    }

    @Override
    public Map<SpeciesKey, Double> getProtectionMap(SpeciesRegistry passedRegistry) {
        if (!redBookProtectionEnabled) {
            return Collections.emptyMap();
        }
        
        SpeciesRegistry activeRegistry = (passedRegistry != null) ? passedRegistry : this.registry;
        if (activeRegistry == null) {
            return Collections.emptyMap();
        }

        Map<SpeciesKey, Double> protectionMap = new HashMap<>();
        int islandArea = width * height;

        for (SpeciesKey key : activeRegistry.getAllAnimalKeys()) {
            AnimalType type = activeRegistry.getAnimalType(key).orElse(null);
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

    @Override
    public void reportDeath(SpeciesKey speciesKey, DeathCause cause) {
        statisticsService.registerDeath(speciesKey, cause);
    }

    @Override
    public void onOrganismAdded(SpeciesKey key) {
        statisticsService.registerBirth(key);
    }

    @Override
    public void onOrganismRemoved(SpeciesKey key) {
        statisticsService.registerRemoval(key);
    }

    @Override
    public int getSpeciesCount(SpeciesKey key) {
        return statisticsService.getSpeciesCount(key);
    }

    @Override
    public com.island.engine.WorldSnapshot createSnapshot() {
        return new IslandSnapshot(this);
    }

    @Override
    public StatisticsService getStatisticsService() {
        return statisticsService;
    }

    public Map<SpeciesKey, Integer> getSpeciesCounts() {
        return statisticsService.getSpeciesCountsMap();
    }

    public int getTotalOrganismCount() {
        return statisticsService.getTotalPopulation();
    }

    public int getTotalAnimalDeathCount(DeathCause cause) {
        return statisticsService.getTotalDeaths(cause).entrySet().stream()
                .filter(e -> !registry.getAnimalType(e.getKey()).map(AnimalType::isBiomass).orElse(false))
                .mapToInt(Map.Entry::getValue)
                .sum();
    }

    public Map<SpeciesKey, Integer> getTotalDeathsBySpecies(DeathCause cause) {
        return statisticsService.getTotalDeaths(cause);
    }

    @Override
    public Collection<? extends Collection<? extends SimulationNode>> getParallelWorkUnits() {
        return chunks.stream().map(Chunk::getCells).toList();
    }

    @Override
    public Optional<SimulationNode> getNode(SimulationNode current, int dx, int dy) {
        if (current instanceof Cell cell) {
            int tx = (cell.getX() + dx % width + width) % width;
            int ty = (cell.getY() + dy % height + height) % height;
            return Optional.of(grid[tx][ty]);
        }
        return Optional.empty();
    }

    public Cell getCell(int x, int y) {
        int tx = (x % width + width) % width;
        int ty = (y % height + height) % height;
        return grid[tx][ty];
    }

    @Override
    public void moveAnimal(Animal animal, SimulationNode from, SimulationNode to) {
        if (from instanceof Cell f && to instanceof Cell t) {
            moveOrganism(animal, f, t);
        }
    }

    @Override
    public void moveBiomassPartially(Biomass b, SimulationNode from, SimulationNode to, double amount) {
        if (from instanceof Cell f && to instanceof Cell t) {
            moveBiomassPartially(b, f, t, amount);
        }
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
                double actualToMove = Math.min(b.getBiomass(), amount);
                if (to.addBiomass(b.getSpeciesKey(), actualToMove)) {
                    b.consumeBiomass(actualToMove, from);
                }
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

    @Override
    public void tick(int tickCount) {
        this.tickCount = tickCount;
        statisticsService.onTickStarted();
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
                        // If failed to add to target, try to return back
                        if (!from.addAnimal(animal)) {
                            // If failed to return, the animal dies (too many in both locations)
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
}
