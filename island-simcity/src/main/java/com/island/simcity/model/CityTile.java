package com.island.simcity.model;

import com.island.simcity.entities.SimEntity;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;

public class CityTile implements SimulationNode<SimEntity> {
    @Getter private final int x;
    @Getter private final int y;
    private final SimulationWorld<SimEntity> world;
    private final List<SimEntity> entities = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    
    @Getter @Setter private List<SimulationNode<SimEntity>> neighbors = new ArrayList<>();
    private boolean connected = false;
    private boolean watered = false;
    private boolean railed = false;
    private boolean metroConnected = false;
    private boolean powered = false;
    @Getter @Setter private int airPollution = 0;
    @Getter @Setter private int waterPollution = 0;

    public void setConnected(boolean connected) { this.connected = connected; }
    public boolean isConnected() { return connected; }
    
    public void setWatered(boolean watered) { this.watered = watered; }
    public boolean isWatered() { return watered; }
    
    public void setRailed(boolean railed) { this.railed = railed; }
    public boolean isRailed() { return railed; }

    public void setMetroConnected(boolean metroConnected) { this.metroConnected = metroConnected; }
    public boolean isMetroConnected() { return metroConnected; }
    
    public void setPowered(boolean powered) { this.powered = powered; }
    public boolean isPowered() { return powered; }

    public CityTile(int x, int y, SimulationWorld<SimEntity> world) {
        this.x = x;
        this.y = y;
        this.world = world;
    }
@Override public List<SimulationNode<SimEntity>> getNeighbors() { return neighbors; }
@Override public void setNeighbors(List<SimulationNode<SimEntity>> neighbors) { this.neighbors = neighbors; }
@Override public Lock getLock() { return lock; }
@Override public SimulationWorld<SimEntity> getWorld() { return world; }
@Override public List<SimEntity> getEntities() { return new ArrayList<>(entities); }
@Override public String getCoordinates() { return x + "," + y; }
@Override public void forEachEntity(Consumer<SimEntity> action) { entities.forEach(action); }
@Override public int getEntityCount() { return entities.size(); }
@Override public boolean canAccept(SimEntity entity) { return true; }
@Override public boolean addEntity(SimEntity entity) { return entities.add(entity); }
@Override public boolean removeEntity(SimEntity entity) { return entities.remove(entity); }
@Override public void cleanupDeadEntities(Consumer<SimEntity> onEntityRemoved) { entities.removeIf(e -> !e.isAlive()); }
    public int getX() { return x; }
    public int getY() { return y; }
}
