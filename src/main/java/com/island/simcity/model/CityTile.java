package com.island.simcity.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.simcity.entities.SimEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@RequiredArgsConstructor
public class CityTile implements SimulationNode<SimEntity> {
    private final int x;
    private final int y;
    private final SimulationWorld<SimEntity> world;
    private final List<SimEntity> entities = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    @Setter
    private List<SimulationNode<SimEntity>> neighbors = Collections.emptyList();
    @Setter
    private boolean connected = false;

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
