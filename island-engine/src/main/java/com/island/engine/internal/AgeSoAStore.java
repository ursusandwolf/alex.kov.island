package com.island.engine.internal;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance, primitive-based store for AgeComponent data.
 * Uses AtomicIntegerArray for thread-safe element access and StampedLock for safe resizing.
 */
@InternalEngine
public final class AgeSoAStore implements AgeStorage {
    private volatile AtomicIntegerArray age;
    private volatile AtomicIntegerArray maxLifespan;
    private volatile int capacity;
    private final StampedLock lock = new StampedLock();

    public AgeSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.age = new AtomicIntegerArray(initialCapacity);
        this.maxLifespan = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, int age, int maxLifespan) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                this.age.set(entityId, age);
                this.maxLifespan.set(entityId, maxLifespan);
                return;
            }
        } finally {
            lock.unlockRead(stamp);
        }

        stamp = lock.writeLock();
        try {
            ensureCapacityInternal(entityId);
            this.age.set(entityId, age);
            this.maxLifespan.set(entityId, maxLifespan);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public int getAge(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicIntegerArray arr = age;
        int val = (entityId < cap) ? arr.get(entityId) : 0;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = age;
                val = (entityId < cap) ? arr.get(entityId) : 0;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return val;
    }

    @Override
    public int getMaxLifespan(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicIntegerArray arr = maxLifespan;
        int val = (entityId < cap) ? arr.get(entityId) : 0;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = maxLifespan;
                val = (entityId < cap) ? arr.get(entityId) : 0;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return val;
    }

    @Override
    public void setAge(int entityId, int age) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                this.age.set(entityId, age);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    @Override
    public void setMaxLifespan(int entityId, int maxLifespan) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                this.maxLifespan.set(entityId, maxLifespan);
            }
        } finally {
            lock.unlockRead(stamp);
        }
    }

    private void ensureCapacityInternal(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            
            AtomicIntegerArray newAge = new AtomicIntegerArray(newCapacity);
            AtomicIntegerArray newMaxLifespan = new AtomicIntegerArray(newCapacity);
            
            for (int i = 0; i < capacity; i++) {
                newAge.set(i, age.get(i));
                newMaxLifespan.set(i, maxLifespan.get(i));
            }
            
            this.age = newAge;
            this.maxLifespan = newMaxLifespan;
            this.capacity = newCapacity;
        }
    }
}
