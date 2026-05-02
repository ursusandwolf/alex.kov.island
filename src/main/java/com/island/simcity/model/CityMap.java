package com.island.simcity.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.WorldSnapshot;
import com.island.simcity.entities.SimEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CityMap implements SimulationWorld<SimEntity> {
    private final int width;
    private final int height;
    private final CityTile[][] grid;
    private volatile long money = 10000;
    private volatile int population = 0;
    
    private volatile int totalJobs = 0;
    private volatile int resDemand = 50;
    private volatile int comDemand = 50;
    private volatile int indDemand = 50;
    private volatile long lastTickIncome = 0;
    private volatile long lastTickExpenses = 0;
    
    private volatile int taxRate = 15;
    private volatile int bankruptcyTicks = 0;
    private static final int BANKRUPTCY_THRESHOLD = 5;
    private final List<String> alerts = new CopyOnWriteArrayList<>();
    private final List<com.island.engine.WorldListener> listeners = new ArrayList<>();

    public CityMap(int width, int height) {
        this.width = width;
        this.height = height;
        this.grid = new CityTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new CityTile(x, y, this);
            }
        }
    }

    @Override
    public Object getConfiguration() {
        return null; // SimCity doesn't use Configuration yet
    }

    @Override
    public void addListener(com.island.engine.WorldListener listener) {
        this.listeners.add(listener);
    }

    @Override
    public List<com.island.engine.WorldListener> getListeners() {
        return this.listeners;
    }

    public synchronized void addMoney(long amount) {
        this.money += amount;
    }

    @Override
    public Collection<? extends Collection<? extends SimulationNode<SimEntity>>> getParallelWorkUnits() {
        List<List<CityTile>> chunks = new ArrayList<>();
        List<CityTile> currentChunk = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                currentChunk.add(grid[x][y]);
                if (currentChunk.size() >= 10) {
                    chunks.add(currentChunk);
                    currentChunk = new ArrayList<>();
                }
            }
        }
        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk);
        }
        return chunks;
    }

    @Override
    public Optional<SimulationNode<SimEntity>> getNode(SimulationNode<SimEntity> current, int dx, int dy) {
        if (current instanceof CityTile tile) {
            int tx = tile.getX() + dx;
            int ty = tile.getY() + dy;
            if (tx >= 0 && tx < width && ty >= 0 && ty < height) {
                return Optional.of(grid[tx][ty]);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean moveEntity(SimEntity entity, SimulationNode<SimEntity> from, SimulationNode<SimEntity> to) {
        if (to.canAccept(entity) && from.removeEntity(entity)) {
            return to.addEntity(entity);
        }
        return false;
    }

    @Override
    public WorldSnapshot createSnapshot() {
        return new CitySnapshot(this);
    }

    @Override
    public void initialize() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                CityTile tile = grid[x][y];
                List<SimulationNode<SimEntity>> neighbors = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        getNode(tile, dx, dy).ifPresent(neighbors::add);
                    }
                }
                tile.setNeighbors(neighbors);
            }
        }
    }

    @Override
    public synchronized void tick(int tickCount) {
        alerts.clear();
        if (money < 0) {
            bankruptcyTicks++;
            alerts.add("CITY BANKRUPT!");
        } else {
            bankruptcyTicks = Math.max(0, bankruptcyTicks - 1);
        }
        if (taxRate > 30) {
            alerts.add("High Taxes!");
        }
        if (resDemand > 50) {
            alerts.add("Housing Shortage!");
        }
    }

    public synchronized void addAlert(String alert) {
        if (!alerts.contains(alert)) {
            alerts.add(alert);
        }
    }

    public synchronized boolean isBankrupt() {
        return bankruptcyTicks >= BANKRUPTCY_THRESHOLD;
    }
}
