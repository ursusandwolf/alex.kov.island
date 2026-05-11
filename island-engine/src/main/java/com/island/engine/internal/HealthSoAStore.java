package com.island.engine.internal;

import com.island.engine.core.HealthStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance, primitive-based store for SoA (Structure of Arrays) components.
 * Uses Atomic arrays for thread-safe element access and StampedLock for safe resizing.
 */
@InternalEngine
public final class HealthSoAStore implements HealthStorage {
    private volatile AtomicLongArray currentEnergy;
    private volatile AtomicLongArray maxEnergy;
    private volatile AtomicIntegerArray alive;
    private volatile int capacity;
    private final StampedLock lock = new StampedLock();

    public HealthSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.currentEnergy = new AtomicLongArray(initialCapacity);
        this.maxEnergy = new AtomicLongArray(initialCapacity);
        this.alive = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, long currentEnergy, long maxEnergy, boolean alive) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                this.currentEnergy.set(entityId, currentEnergy);
                this.maxEnergy.set(entityId, maxEnergy);
                this.alive.set(entityId, alive ? 1 : 0);
                return;
            }
        } finally {
            lock.unlockRead(stamp);
        }

        stamp = lock.writeLock();
        try {
            ensureCapacityInternal(entityId);
            this.currentEnergy.set(entityId, currentEnergy);
            this.maxEnergy.set(entityId, maxEnergy);
            this.alive.set(entityId, alive ? 1 : 0);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public long getCurrentEnergy(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicLongArray arr = currentEnergy;
        long val = (entityId < cap) ? arr.get(entityId) : 0L;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = currentEnergy;
                val = (entityId < cap) ? arr.get(entityId) : 0L;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return val;
    }

    @Override
    public long getMaxEnergy(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicLongArray arr = maxEnergy;
        long val = (entityId < cap) ? arr.get(entityId) : 0L;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = maxEnergy;
                val = (entityId < cap) ? arr.get(entityId) : 0L;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return val;
    }

    @Override
    public boolean isAlive(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicIntegerArray arr = alive;
        boolean val = (entityId < cap) && arr.get(entityId) == 1;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = alive;
                val = (entityId < cap) && arr.get(entityId) == 1;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return val;
    }

    @Override
    public void setCurrentEnergy(int entityId, long energy) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                currentEnergy.set(entityId, energy);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void setAlive(int entityId, boolean isAlive) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                alive.set(entityId, isAlive ? 1 : 0);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public long addEnergy(int entityId, long delta) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                return currentEnergy.addAndGet(entityId, delta);
            }
        } finally {
            lock.unlockRead(stamp);
        }
        return 0L;
    }

    private void ensureCapacityInternal(int entityId) {
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
