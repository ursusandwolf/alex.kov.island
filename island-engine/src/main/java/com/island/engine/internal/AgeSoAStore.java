package com.island.engine.internal;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.InternalEngine;

/**
 * High-performance, primitive-based store for AgeComponent data.
 */
@InternalEngine
public class AgeSoAStore implements AgeStorage {
    private int[] age;
    private int[] maxLifespan;
    private int capacity;

    public AgeSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.age = new int[initialCapacity];
        this.maxLifespan = new int[initialCapacity];
    }

    @Override
    public void set(int entityId, int age, int maxLifespan) {
        ensureCapacity(entityId);
        this.age[entityId] = age;
        this.maxLifespan[entityId] = maxLifespan;
    }

    public int getAge(int entityId) { return age[entityId]; }
    public int getMaxLifespan(int entityId) { return maxLifespan[entityId]; }

    public void setAge(int entityId, int age) { this.age[entityId] = age; }
    public void setMaxLifespan(int entityId, int maxLifespan) { this.maxLifespan[entityId] = maxLifespan; }

    private void ensureCapacity(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            age = java.util.Arrays.copyOf(age, newCapacity);
            maxLifespan = java.util.Arrays.copyOf(maxLifespan, newCapacity);
            capacity = newCapacity;
        }
    }
}
