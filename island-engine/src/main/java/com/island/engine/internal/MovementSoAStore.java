package com.island.engine.internal;

import com.island.engine.core.MovementStorage;
import com.island.engine.core.InternalEngine;
import java.util.Arrays;

/**
 * High-performance, primitive-based store for movement SoA components.
 */
@InternalEngine
public class MovementSoAStore implements MovementStorage {
    private volatile int[] speeds;
    private volatile int capacity;

    public MovementSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.speeds = new int[initialCapacity];
    }

    @Override
    public void set(int entityId, int speed) {
        ensureCapacity(entityId);
        this.speeds[entityId] = speed;
    }

    @Override
    public int getSpeed(int entityId) {
        if (entityId >= capacity) return 0;
        return speeds[entityId];
    }

    @Override
    public void setSpeed(int entityId, int speed) {
        ensureCapacity(entityId);
        this.speeds[entityId] = speed;
    }

    private synchronized void ensureCapacity(int entityId) {
        if (entityId >= capacity) {
            int newCapacity = Math.max(entityId + 1, capacity * 2);
            speeds = Arrays.copyOf(speeds, newCapacity);
            capacity = newCapacity;
        }
    }
}
