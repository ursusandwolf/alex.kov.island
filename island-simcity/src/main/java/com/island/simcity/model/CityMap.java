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
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class CityMap implements SimulationWorld<SimEntity> {
    private final int width;
    private final int height;
    private final CityTile[][] grid;
    @Setter private long money = 10000;
    @Setter private int population = 0;
    @Setter private int totalJobs = 0;
    @Setter private int resDemand = 50;
    @Setter private int comDemand = 50;
    @Setter private int indDemand = 50;
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
    }

    @Override
    public ComponentRegistry getComponentRegistry() { return componentRegistry; }
    
    @Override
    public EventBus getEventBus() { return eventBus; }
    
    @Override
    public int getHeight() { return height; }
    
    @Override
    public int getWidth() { return width; }

    public CityTile[][] getGrid() { return grid; }
    public long getMoney() { return money; }
    public int getTaxRate() { return taxRate; }
    public long getLastTickIncome() { return lastTickIncome; }
    public long getLastTickExpenses() { return lastTickExpenses; }
    public List<String> getAlerts() { return alerts; }
    public int getResDemand() { return resDemand; }
    public int getComDemand() { return comDemand; }
    public int getIndDemand() { return indDemand; }
    public int getTotalJobs() { return totalJobs; }

    public void setPopulation(int population) { this.population = population; }
    public void setTotalJobs(int totalJobs) { this.totalJobs = totalJobs; }
    public void setResDemand(int resDemand) { this.resDemand = resDemand; }
    public void setIndDemand(int indDemand) { this.indDemand = indDemand; }
    public void setComDemand(int comDemand) { this.comDemand = comDemand; }
    public void setLastTickIncome(long income) { this.lastTickIncome = income; }
    public void setLastTickExpenses(long expenses) { this.lastTickExpenses = expenses; }
    public void addAlert(String alert) { alerts.add(alert); }
    public boolean isBankrupt() { return money < 0; }
    public void addMoney(long amount) { this.money += amount; }
    
    @Override
    public void onEntityAdded(SimEntity entity) {}
    @Override
    public void onEntityRemoved(SimEntity entity) {}
    @Override
    public void tick(int tickCount) {}
    @Override
    public void initialize() {}
    @Override
    public Collection<? extends WorkUnit<SimEntity>> getParallelWorkUnits() { return List.of(); }
    @Override
    public Optional<SimulationNode<SimEntity>> getNode(SimulationNode<SimEntity> current, int dx, int dy) { return Optional.empty(); }
    @Override
    public boolean moveEntity(SimEntity entity, SimulationNode<SimEntity> from, SimulationNode<SimEntity> to) { return false; }
    @Override
    public WorldSnapshot createSnapshot() { return null; }
}
