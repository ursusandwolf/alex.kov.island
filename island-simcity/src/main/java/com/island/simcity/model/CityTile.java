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
    @Getter private final SimulationWorld<SimEntity> world;
    private final List<SimEntity> entities = new ArrayList<>();
    @Getter private final Lock lock = new ReentrantLock();
    
    @Getter @Setter private List<SimulationNode<SimEntity>> neighbors = new ArrayList<>();
    @Getter @Setter private boolean connected = false;
    @Getter @Setter private boolean watered = false;
    @Getter @Setter private boolean railed = false;
    @Getter @Setter private boolean metroConnected = false;
    @Getter @Setter private boolean powered = false;
    @Getter @Setter private int airPollution = 0;
    @Getter @Setter private int waterPollution = 0;
    @Getter @Setter private int desirability = 0;
    @Getter @Setter private int educationLevel = 0;
    @Getter @Setter private int healthLevel = 0;

    public CityTile(int x, int y, SimulationWorld<SimEntity> world) {
        this.x = x;
        this.y = y;
        this.world = world;
    }

    @Override
    public List<SimEntity> getEntities() {
        lock.lock();
        try {
            return new ArrayList<>(entities);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    @Override
    public void forEachEntity(Consumer<SimEntity> action) {
        lock.lock();
        try {
            entities.forEach(action);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int getEntityCount() {
        lock.lock();
        try {
            return entities.size();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean canAccept(SimEntity entity) {
        return true;
    }

    @Override
    public boolean addEntity(SimEntity entity) {
        lock.lock();
        try {
            return entities.add(entity);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeEntity(SimEntity entity) {
        lock.lock();
        try {
            return entities.remove(entity);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cleanupDeadEntities(Consumer<SimEntity> onEntityRemoved) {
        lock.lock();
        try {
            entities.removeIf(e -> {
                if (!e.isAlive()) {
                    onEntityRemoved.accept(e);
                    return true;
                }
                return false;
            });
        } finally {
            lock.unlock();
        }
    }
}
