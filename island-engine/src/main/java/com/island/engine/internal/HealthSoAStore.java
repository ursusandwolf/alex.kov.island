package com.island.engine.internal;

import com.island.engine.core.HealthStorage;
import com.island.engine.core.InternalEngine;

/**
 * High-performance, primitive-based store for SoA (Structure of Arrays) components.
 * Designed to minimize GC pressure and improve cache locality for mass entities.
 */
@InternalEngine
public class HealthSoAStore implements HealthStorage {
    private volatile long[] currentEnergy;
    private volatile long[] maxEnergy;
    private volatile boolean[] alive;
    private volatile int capacity;

    public HealthSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.currentEnergy = new long[initialCapacity];
        this.maxEnergy = new long[initialCapacity];
        this.alive = new boolean[initialCapacity];
    }

    @Override
    public void set(int entityId, long currentEnergy, long maxEnergy, boolean alive) {
        ensureCapacity(entityId);
        this.currentEnergy[entityId] = currentEnergy;
        this.maxEnergy[entityId] = maxEnergy;
        this.alive[entityId] = alive;
    }

    public long getCurrentEnergy(int entityId) { return currentEnergy[entityId]; }
    public long getMaxEnergy(int entityId) { return maxEnergy[entityId]; }
    public boolean isAlive(int entityId) { return alive[entityId]; }

    public void setCurrentEnergy(int entityId, long energy) { this.currentEnergy[entityId] = energy; }
    public void setAlive(int entityId, boolean isAlive) { this.alive[entityId] = isAlive; }

    private synchronized void ensureCapacity(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            currentEnergy = java.util.Arrays.copyOf(currentEnergy, newCapacity);
            maxEnergy = java.util.Arrays.copyOf(maxEnergy, newCapacity);
            alive = java.util.Arrays.copyOf(alive, newCapacity);
            capacity = newCapacity;
        }
    }
}
