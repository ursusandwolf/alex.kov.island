package com.island.engine.internal;

import com.island.engine.core.HealthStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * High-performance, primitive-based store for SoA (Structure of Arrays) components.
 * Uses Atomic arrays for thread-safe element access.
 */
@InternalEngine
public final class HealthSoAStore implements HealthStorage {
    private volatile AtomicLongArray currentEnergy;
    private volatile AtomicLongArray maxEnergy;
    private volatile AtomicIntegerArray alive;
    private volatile int capacity;

    public HealthSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.currentEnergy = new AtomicLongArray(initialCapacity);
        this.maxEnergy = new AtomicLongArray(initialCapacity);
        this.alive = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, long currentEnergy, long maxEnergy, boolean alive) {
        ensureCapacity(entityId);
        this.currentEnergy.set(entityId, currentEnergy);
        this.maxEnergy.set(entityId, maxEnergy);
        this.alive.set(entityId, alive ? 1 : 0);
    }

    @Override
    public long getCurrentEnergy(int entityId) {
        int cap = capacity;
        AtomicLongArray arr = currentEnergy;
        return (entityId < cap) ? arr.get(entityId) : 0L;
    }

    @Override
    public long getMaxEnergy(int entityId) {
        int cap = capacity;
        AtomicLongArray arr = maxEnergy;
        return (entityId < cap) ? arr.get(entityId) : 0L;
    }

    @Override
    public boolean isAlive(int entityId) {
        int cap = capacity;
        AtomicIntegerArray arr = alive;
        return (entityId < cap) && arr.get(entityId) == 1;
    }

    @Override
    public void setCurrentEnergy(int entityId, long energy) {
        int cap = capacity;
        AtomicLongArray arr = currentEnergy;
        if (entityId < cap) {
            arr.set(entityId, energy);
        }
    }

    @Override
    public void setAlive(int entityId, boolean isAlive) {
        int cap = capacity;
        AtomicIntegerArray arr = alive;
        if (entityId < cap) {
            arr.set(entityId, isAlive ? 1 : 0);
        }
    }

    @Override
    public long addEnergy(int entityId, long delta) {
        int cap = capacity;
        AtomicLongArray arr = currentEnergy;
        if (entityId < cap) {
            return arr.addAndGet(entityId, delta);
        }
        return 0L;
    }

    private synchronized void ensureCapacity(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            
            AtomicLongArray newCurrentEnergy = new AtomicLongArray(newCapacity);
            AtomicLongArray newMaxEnergy = new AtomicLongArray(newCapacity);
            AtomicIntegerArray newAlive = new AtomicIntegerArray(newCapacity);
            
            for (int i = 0; i < capacity; i++) {
                newCurrentEnergy.set(i, currentEnergy.get(i));
                newMaxEnergy.set(i, maxEnergy.get(i));
                newAlive.set(i, alive.get(i));
            }
            
            this.currentEnergy = newCurrentEnergy;
            this.maxEnergy = newMaxEnergy;
            this.alive = newAlive;
            this.capacity = newCapacity;
        }
    }
}
