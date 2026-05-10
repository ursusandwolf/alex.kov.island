package com.island.engine.internal;

import com.island.engine.core.MovementStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicIntegerArray;

/**
 * High-performance, primitive-based store for movement SoA components.
 * Uses AtomicIntegerArray for thread-safe element access.
 */
@InternalEngine
public class MovementSoAStore implements MovementStorage {
    private volatile AtomicIntegerArray speeds;
    private volatile int capacity;

    public MovementSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.speeds = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, int speed) {
        ensureCapacity(entityId);
        this.speeds.set(entityId, speed);
    }

    @Override
    public int getSpeed(int entityId) {
        if (entityId >= capacity) return 0;
        return speeds.get(entityId);
    }

    @Override
    public void setSpeed(int entityId, int speed) {
        set(entityId, speed);
    }

    private synchronized void ensureCapacity(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            AtomicIntegerArray newSpeeds = new AtomicIntegerArray(newCapacity);
            for (int i = 0; i < capacity; i++) {
                newSpeeds.set(i, speeds.get(i));
            }
            this.speeds = newSpeeds;
            this.capacity = newCapacity;
        }
    }
}
