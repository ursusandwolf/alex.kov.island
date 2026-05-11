package com.island.nature.model;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.HealthStorage;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.event.AnimalBornEvent;
import com.island.nature.event.AnimalDiedEvent;
import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.model.WorldSnapshot;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.AnimalType;
import com.island.nature.entities.core.Biomass;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.domain.NatureWorld;
import com.island.nature.entities.environment.Season;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.math.GridUtils;

@Getter
public class Island implements NatureWorld {
    private final NatureDomainContext domainContext;
    private final Configuration config;
    private final int width;
    private final int height;
    private final Cell[][] grid;
    private final List<Chunk> chunks = new ArrayList<>();
    private final SpeciesRegistry registry;
    private final ComponentRegistry componentRegistry;
    private final StatisticsService statisticsService;
    private final ProtectionService protectionService;
    private int tickCount = 0;
    @Setter private boolean redBookProtectionEnabled = true;
    private final EventBus eventBus;

    public Island(NatureDomainContext domainContext, int width, int height, EventBus eventBus) {
        this.domainContext = domainContext;
        this.config = domainContext.getConfig();
        this.width = width;
        this.height = height;
        this.eventBus = eventBus;
        this.registry = domainContext.getSpeciesRegistry();
        this.componentRegistry = domainContext.getComponentRegistry();
        this.statisticsService = domainContext.getStatisticsService();
        this.protectionService = domainContext.getProtectionService();
        this.grid = new Cell[width][height];
        initializeGrid();
        partitionIntoChunks();
    }

    public void init() {
        initNeighbors();
    }

    @Override
    public void onEntityAdded(Organism entity) {
        if (entity instanceof Animal a && eventBus != null) {
            eventBus.publish(new AnimalBornEvent(a));
        }
    }

    @Override
    public void onEntityRemoved(Organism entity) {
        if (entity instanceof Animal a && eventBus != null) {
            DeathCause cause = a.getLastDeathCause();
            eventBus.publish(new AnimalDiedEvent(a, (cause != null) ? cause : DeathCause.HUNGER));
        }
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
    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }

    @Override
    public HealthStorage getHealthStorage() {
        return domainContext.getHealthStorage();
    }

    @Override
    public AgeStorage getAgeStorage() {
        return domainContext.getAgeStorage();
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
    public Collection<? extends WorkUnit<Organism>> getParallelWorkUnits() {
        return chunks;
    }

    @Override
    public Optional<SimulationNode<Organism>> getNode(SimulationNode<Organism> current, int dx, int dy) {
        if (current instanceof Cell cell) {
            return getCell(cell, dx, dy).map(c -> c);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Cell> getCell(Cell current, int dx, int dy) {
        int tx = current.getX() + dx;
        int ty = current.getY() + dy;
        if (GridUtils.isValid(tx, ty, width, height)) {
            return Optional.of(grid[tx][ty]);
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
                GridUtils.executeWithDoubleLock(f, t, () -> {
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
        GridUtils.executeWithDoubleLock(from, to, () -> {
            long actualToMove = Math.min(b.getBiomass(), amount);
            if (to.addBiomass(b.getSpeciesKey(), actualToMove)) {
                b.consumeBiomass(actualToMove, from);
            }
        });
    }

    @Override
    public Season getCurrentSeason() {
        return domainContext.getClimateService().getCurrentSeason();
    }

    @Override
    public int getTemperature() {
        return domainContext.getClimateService().getTemperature();
    }

    @Override
    public void tick(int tickCount) {
        this.tickCount = tickCount;
        statisticsService.onTickStarted();
        protectionService.update(tickCount);
        
        if (config.isDynamicChunkingEnabled() && tickCount % config.getRebalanceInterval() == 0) {
            rebalance();
        }
    }

    @Override
    public void rebalance() {
        this.chunks.clear();
        partitionIntoChunks();
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new Cell(x, y, this);
            }
        }
    }

    private void partitionIntoChunks() {
        this.chunks.addAll(domainContext.getChunkingStrategy().partition(width, height, this));
    }

    @Override
    public boolean moveOrganism(Animal animal, Cell from, Cell to) {
        if (from == to) {
            return true;
        }
        final boolean[] result = {false};
        GridUtils.executeWithDoubleLock(from, to, () -> {
            if (to.canAcceptInternal(animal)) {
                if (from.removeAnimalInternal(animal)) {
                    if (to.addAnimalInternal(animal, true)) {
                        result[0] = true;
                    } else {
                        from.addAnimalInternal(animal, true);
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
