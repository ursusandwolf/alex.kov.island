package com.island.simcity.model;

import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.core.DefaultWorkUnit;
import com.island.engine.model.WorldSnapshot;
import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.EventBus;
import com.island.util.math.GridUtils;
import com.island.simcity.entities.SimEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CityMap implements SimulationWorld<SimEntity> {
    private final int width;
    private final int height;
    private final CityTile[][] grid;
    private final AtomicLong money = new AtomicLong(10000);
    @Setter private int population = 0;
    @Setter private int totalJobs = 0;
    @Setter private int resDemand = 50;
    @Setter private int comDemand = 50;
    @Setter private int indDemand = 50;
    @Setter private int averageEQ = 0;
    @Setter private int averageHealth = 0;
    @Setter private long lastTickIncome = 0;
    @Setter private long lastTickExpenses = 0;
    @Setter private int taxRate = 15;
    
    private final List<String> alerts = new CopyOnWriteArrayList<>();
    private final EventBus eventBus;
    private final ComponentRegistry componentRegistry;

    public CityMap(int width, int height, EventBus eventBus, ComponentRegistry registry) {
        this.width = width;
        this.height = height;
        this.eventBus = eventBus;
        this.componentRegistry = registry;
        this.grid = new CityTile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new CityTile(x, y, this);
            }
        }
        
        // Initialize neighbors
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                List<com.island.engine.core.SimulationNode<SimEntity>> neighbors = new ArrayList<>();
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) continue;
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            neighbors.add(grid[nx][ny]);
                        }
                    }
                }
                grid[x][y].setNeighbors(neighbors);
            }
        }
    }

    public long getMoney() { return money.get(); }
    public void setMoney(long amount) { this.money.set(amount); }

    private int negativeBalanceTicks = 0;
    @Getter private boolean bankrupt = false;

    public void addAlert(String alert) { alerts.add(alert); }
    public void addMoney(long amount) { this.money.addAndGet(amount); }
    
    @Override
    public void onEntityAdded(SimEntity entity) {}
    @Override
    public void onEntityRemoved(SimEntity entity) {}
    @Override
    public void tick(int tickCount) {
        if (money.get() < 0) {
            negativeBalanceTicks++;
            if (negativeBalanceTicks >= 5) {
                if (!bankrupt) {
                    bankrupt = true;
                    addAlert("CITY BANKRUPT!");
                }
            }
        } else {
            negativeBalanceTicks = 0;
            bankrupt = false;
        }
    }
    @Override
    public void initialize() {}
    @Override
    public Collection<? extends WorkUnit<SimEntity>> getParallelWorkUnits() {
        List<CityTile> allTiles = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                allTiles.add(grid[x][y]);
            }
        }
        return List.of(new DefaultWorkUnit<>(allTiles));
    }
    @Override
    public Optional<SimulationNode<SimEntity>> getNode(SimulationNode<SimEntity> current, int dx, int dy) { return Optional.empty(); }
    @Override
    public boolean moveEntity(SimEntity entity, SimulationNode<SimEntity> from, SimulationNode<SimEntity> to) { return false; }
    @Override
    public WorldSnapshot createSnapshot() { return null; }
}
