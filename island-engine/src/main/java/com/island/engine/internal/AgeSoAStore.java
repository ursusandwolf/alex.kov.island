package com.island.engine.internal;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * High-performance, primitive-based store for AgeComponent data.
 * Uses AtomicIntegerArray for thread-safe element access.
 */
@InternalEngine
public final class AgeSoAStore implements AgeStorage {
    private volatile AtomicIntegerArray age;
    private volatile AtomicIntegerArray maxLifespan;
    private volatile int capacity;

    public AgeSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.age = new AtomicIntegerArray(initialCapacity);
        this.maxLifespan = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, int age, int maxLifespan) {
        ensureCapacity(entityId);
        this.age.set(entityId, age);
        this.maxLifespan.set(entityId, maxLifespan);
    }

    @Override
    public int getAge(int entityId) {
        int cap = capacity;
        AtomicIntegerArray arr = age;
        return (entityId < cap) ? arr.get(entityId) : 0;
    }

    @Override
    public int getMaxLifespan(int entityId) {
        int cap = capacity;
        AtomicIntegerArray arr = maxLifespan;
        return (entityId < cap) ? arr.get(entityId) : 0;
    }

    @Override
    public void setAge(int entityId, int age) {
        int cap = capacity;
        AtomicIntegerArray arr = this.age;
        if (entityId < cap) {
            arr.set(entityId, age);
        }
    }

    @Override
    public void setMaxLifespan(int entityId, int maxLifespan) {
        int cap = capacity;
        AtomicIntegerArray arr = this.maxLifespan;
        if (entityId < cap) {
            arr.set(entityId, maxLifespan);
        }
    }

    private synchronized void ensureCapacity(int entityId) {
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
