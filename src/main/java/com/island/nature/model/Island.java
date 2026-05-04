package com.island.nature.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.WorldListener;
import com.island.engine.WorldSnapshot;
import com.island.engine.event.EntityBornEvent;
import com.island.engine.event.EntityDiedEvent;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.Animal;
import com.island.nature.entities.AnimalType;
import com.island.nature.entities.Biomass;
import com.island.nature.entities.DeathCause;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.NatureWorld;
import com.island.nature.entities.Organism;
import com.island.nature.entities.Season;
import com.island.nature.entities.SpeciesKey;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.GridUtils;
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
public class Island implements NatureWorld, WorldListener<Organism> {
    private final Configuration config;
    private final int width;
    private final int height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private final SpeciesRegistry registry;
    private final StatisticsService statisticsService;
    private final ProtectionService protectionService;
    private final List<WorldListener<Organism>> listeners = new ArrayList<>();
    private int tickCount = 0;
    @Setter private boolean redBookProtectionEnabled = true;
    private Season currentSeason = Season.SPRING;
    @Setter private EventBus eventBus;

    public Island(NatureDomainContext domainContext, int width, int height) {
        this.config = domainContext.getConfig();
        this.width = width;
        this.height = height;
        this.registry = domainContext.getSpeciesRegistry();
        this.statisticsService = domainContext.getStatisticsService();
        this.protectionService = domainContext.getProtectionService();
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
        this.addListener(this);
    }

    @Override
    public void addListener(WorldListener<Organism> listener) {
        this.listeners.add(listener);
    }

    @Override
    public List<WorldListener<Organism>> getListeners() {
        return this.listeners;
    }

    @Override
    public void onEntityAdded(Organism entity) {
        if (entity instanceof Animal a && eventBus != null) {
            eventBus.publish(new EntityBornEvent(a));
        }
    }

    @Override
    public void onEntityRemoved(Organism entity) {
        if (entity instanceof Animal a && eventBus != null) {
            DeathCause cause = a.getLastDeathCause();
            eventBus.publish(new EntityDiedEvent(a, (cause != null) ? cause.name() : "REMOVED"));
        }
    }

    public void init() {
        initNeighbors();
    }

    private void initNeighbors() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Cell cell = grid[x][y];
                cell.setNeighbors(GridUtils.getNeighbors(this, cell, width, height));
            }
        }
    }

    @Override
    public Configuration getConfiguration() {
        return config;
    }

    public Map<SpeciesKey, Integer> getProtectionMap() {
        if (!redBookProtectionEnabled) {
            return Collections.emptyMap();
        }
        return protectionService.getProtectionModifiers();
    }

    public SpeciesRegistry getRegistry() {
        return registry;
    }

    public int getSpeciesCount(SpeciesKey key) {
        return statisticsService.getSpeciesCount(key);
    }

    @Override
    public WorldSnapshot createSnapshot() {
        return new IslandSnapshot(this);
    }

    public ProtectionService getProtectionService() {
        return protectionService;
    }

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
    public Collection<? extends Collection<? extends SimulationNode<Organism>>> getParallelWorkUnits() {
        return chunks.stream().map(Chunk::getCells).toList();
    }

    @Override
    public Optional<SimulationNode<Organism>> getNode(SimulationNode<Organism> current, int dx, int dy) {
        if (current instanceof Cell cell) {
            int tx = cell.getX() + dx;
            int ty = cell.getY() + dy;
            if (GridUtils.isValid(tx, ty, width, height)) {
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
    public boolean moveEntity(Organism entity, SimulationNode<Organism> from, SimulationNode<Organism> to) {
        if (from instanceof Cell f && to instanceof Cell t) {
            if (entity instanceof Animal a) {
                return moveOrganism(a, f, t);
            } else if (entity instanceof Biomass b) {
                final boolean[] result = {false};
                GridUtils.executeWithDoubleLock(f, t, f.getX(), f.getY(), t.getX(), t.getY(), () -> {
                    if (t.addEntity(b)) {
                        if (f.removeEntity(b)) {
                            result[0] = true;
                        }
                    }
                });
                return result[0];
            }
        }
        return false;
    }

    public void moveBiomassPartially(Biomass b, SimulationNode<Organism> from, SimulationNode<Organism> to, long amount) {
        if (from instanceof Cell f && to instanceof Cell t) {
            moveBiomassPartially(b, f, t, amount);
        }
    }

    public void moveBiomassPartially(Biomass b, Cell from, Cell to, long amount) {
        if (from == to || amount <= 0 || b.getBiomass() <= 0) {
            return;
        }
        GridUtils.executeWithDoubleLock(from, to, from.getX(), from.getY(), to.getX(), to.getY(), () -> {
            long actualToMove = Math.min(b.getBiomass(), amount);
            if (to.addBiomass(b.getSpeciesKey(), actualToMove)) {
                b.consumeBiomass(actualToMove, from);
            }
        });
    }

    @Override
    public void tick(int tickCount) {
        this.tickCount = tickCount;
        updateSeason();
        statisticsService.onTickStarted();
        protectionService.update(tickCount);
    }

    private void updateSeason() {
        int seasonDuration = config.getSeasonDuration();
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
        int processors = Runtime.getRuntime().availableProcessors();
        int totalCells = width * height;

        int targetTasks;
        if (totalCells <= 64) {
            targetTasks = 16;
        } else if (totalCells <= processors * 16) {
            targetTasks = processors * 2;
        } else {
            targetTasks = processors * 4;
        }

        targetTasks = Math.min(targetTasks, totalCells);
        int cellsPerChunk = Math.max(1, totalCells / targetTasks);
        int chunkSize = (int) Math.sqrt(cellsPerChunk);

        if (chunkSize < 1) {
            chunkSize = 1;
        }

        if (totalCells > 1000 && chunkSize > 32) {
            chunkSize = 32;
        }

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
        final boolean[] result = {false};
        GridUtils.executeWithDoubleLock(from, to, from.getX(), from.getY(), to.getX(), to.getY(), () -> {
            if (to.canAccept(animal)) {
                if (from.removeAnimal(animal)) {
                    if (to.addAnimal(animal)) {
                        result[0] = true;
                    } else {
                        from.addAnimal(animal);
                    }
                }
            }
        });
        return result[0];
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }
}
