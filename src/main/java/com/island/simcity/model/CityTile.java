package com.island.simcity.model;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.simcity.entities.SimEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CityTile implements SimulationNode<SimEntity> {
    private final int x;
    private final int y;
    private final SimulationWorld<SimEntity> world;
    private final List<SimEntity> entities = new CopyOnWriteArrayList<>();
    private final Lock lock = new ReentrantLock();
    private List<SimulationNode<SimEntity>> neighbors = Collections.emptyList();
    @lombok.Setter
    private boolean connected = false;

    @Override
    public Lock getLock() {
        return lock;
    }

    @Override
    public String getCoordinates() {
        return x + "," + y;
    }

    @Override
    public void setNeighbors(List<SimulationNode<SimEntity>> neighbors) {
        this.neighbors = neighbors;
    }

    @Override
    public List<SimulationNode<SimEntity>> getNeighbors() {
        return neighbors;
    }

    @Override
    public List<SimEntity> getEntities() {
        return entities;
    }

    @Override
    public void forEachEntity(Consumer<SimEntity> action) {
        entities.forEach(action);
    }

    @Override
    public int getEntityCount() {
        return entities.size();
    }

    @Override
    public boolean canAccept(SimEntity entity) {
        return true;
    }

    @Override
    public boolean addEntity(SimEntity entity) {
        return entities.add(entity);
    }

    @Override
    public boolean removeEntity(SimEntity entity) {
        return entities.remove(entity);
    }

    @Override
    public void cleanupDeadEntities(Consumer<SimEntity> onEntityRemoved) {
        entities.removeIf(e -> {
            if (!e.isAlive()) {
                onEntityRemoved.accept(e);
                return true;
            }
            return false;
        });
    }
}
