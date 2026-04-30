package com.island.model;

import com.island.engine.Season;
import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.WorldSnapshot;
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
import lombok.Setter;

@Getter
public class Island implements SimulationWorld {
    private final int width;
    private final int height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final com.island.service.ProtectionService protectionService;
    private int tickCount = 0;
    @Setter private boolean redBookProtectionEnabled = true;
    private Season currentSeason = Season.SPRING;

    public Island(int width, int height, SpeciesRegistry registry, StatisticsService statisticsService) {
        this.width = width;
        this.height = height;
        this.registry = registry;
        this.statisticsService = statisticsService;
        this.protectionService = new com.island.service.DefaultProtectionService(registry, statisticsService, width * height);
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

    @Override
    public Map<SpeciesKey, Integer> getProtectionMap(SpeciesRegistry passedRegistry) {
        if (!redBookProtectionEnabled) {
            return Collections.emptyMap();
        }
        return protectionService.getProtectionModifiers();
    }

    @Override
    public SpeciesRegistry getRegistry() {
        return registry;
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
    public WorldSnapshot createSnapshot() {
        return new IslandSnapshot(this);
    }

    @Override
    public com.island.service.ProtectionService getProtectionService() {
        return protectionService;
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
            int tx = cell.getX() + dx;
            int ty = cell.getY() + dy;
            if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
                return Optional.of(grid[tx][ty]);
            }
        }
        return Optional.empty();
    }

    public Cell getCell(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Coordinates out of island bounds: " + x + "," + y);
        }
        return grid[x][y];
    }

    @Override
    public boolean moveAnimal(Animal animal, SimulationNode from, SimulationNode to) {
        if (from instanceof Cell f && to instanceof Cell t) {
            return moveOrganism(animal, f, t);
        }
        return false;
    }

    @Override
    public void moveBiomassPartially(Biomass b, SimulationNode from, SimulationNode to, long amount) {
        if (from instanceof Cell f && to instanceof Cell t) {
            moveBiomassPartially(b, f, t, amount);
        }
    }

    public void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount) {
        if (from == to || amount <= 0 || b.getBiomass() <= 0) {
            return;
        }
        Cell first = (from.getX() < to.getX() || (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
        Cell second = (first == from) ? to : from;
        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                long actualToMove = Math.min(b.getBiomass(), amount);
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
        updateSeason();
        statisticsService.onTickStarted();
        protectionService.update(tickCount);
    }

    private void updateSeason() {
        int seasonDuration = 50;
        int seasonIndex = (tickCount / seasonDuration) % 4;
        this.currentSeason = Season.values()[seasonIndex];
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

    public boolean moveOrganism(Animal animal, Cell from, Cell to) {
        if (from == to) {
            return true;
        }
        Cell first = (from.getX() < to.getX() || (from.getX() == to.getX() && from.getY() < to.getY())) ? from : to;
        Cell second = (first == from) ? to : from;
        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (to.canAccept(animal)) {
                    if (from.removeAnimal(animal)) {
                        if (to.addAnimal(animal)) {
                            return true;
                        } else {
                            from.addAnimal(animal);
                        }
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
}
