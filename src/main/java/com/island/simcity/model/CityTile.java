package com.island.simcity.model;

import com.island.simcity.entities.Resident;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.event.ResidentBornEvent;
import com.island.simcity.event.ResidentDiedEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;

@RequiredArgsConstructor
public class CityTile implements SimulationNode<SimEntity> {
    @Getter
    private final int x;
    @Getter
    private final int y;
    private final SimulationWorld<SimEntity> world;
    private final List<SimEntity> entities = new ArrayList<>();
    @Getter
    private final Lock lock = new ReentrantLock();
    @Getter
    @Setter
    private List<SimulationNode<SimEntity>> neighbors = Collections.emptyList();
    @Getter
    @Setter
    private boolean connected = false;

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
    public SimulationWorld<SimEntity> getWorld() {
        return world;
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
            if (entities.add(entity)) {
                world.onEntityAdded(entity);
                if (entity instanceof Resident resident) {
                    world.getEventBus().publish(new ResidentBornEvent(resident));
                }
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean removeEntity(SimEntity entity) {
        lock.lock();
        try {
            if (entities.remove(entity)) {
                world.onEntityRemoved(entity);
                if (entity instanceof Resident resident) {
                    world.getEventBus().publish(new ResidentDiedEvent(resident));
                }
                return true;
            }
            return false;
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
                    world.onEntityRemoved(e);
                    if (e instanceof Resident resident) {
                        world.getEventBus().publish(new ResidentDiedEvent(resident));
                    }
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
