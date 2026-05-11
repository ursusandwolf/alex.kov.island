package com.island.engine.internal;

import com.island.engine.core.MovementStorage;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.StampedLock;

/**
 * High-performance, primitive-based store for movement SoA components.
 * Uses AtomicIntegerArray for thread-safe element access and StampedLock for safe resizing.
 * 
 * <p><b>Important:</b> StampedLock is NOT reentrant. Methods in this class must not be 
 * called recursively or from contexts that already hold a write lock on this instance 
 * to avoid deadlocks.
 */
@InternalEngine
public final class MovementSoAStore implements MovementStorage {
    private volatile AtomicIntegerArray speeds;
    private volatile int capacity;
    private final StampedLock lock = new StampedLock();

    public MovementSoAStore(int initialCapacity) {
        this.capacity = initialCapacity;
        this.speeds = new AtomicIntegerArray(initialCapacity);
    }

    @Override
    public void set(int entityId, int speed) {
        long stamp = lock.readLock();
        try {
            if (entityId < capacity) {
                this.speeds.set(entityId, speed);
                return;
            }
        } finally {
            lock.unlockRead(stamp);
        }

        // Need expansion
        stamp = lock.writeLock();
        try {
            ensureCapacityInternal(entityId);
            this.speeds.set(entityId, speed);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    @Override
    public int getSpeed(int entityId) {
        long stamp = lock.tryOptimisticRead();
        int cap = capacity;
        AtomicIntegerArray arr = speeds;
        int speed = (entityId < cap) ? arr.get(entityId) : 0;
        
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                cap = capacity;
                arr = speeds;
                speed = (entityId < cap) ? arr.get(entityId) : 0;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return speed;
    }

    @Override
    public void setSpeed(int entityId, int speed) {
        set(entityId, speed);
    }

    private void ensureCapacityInternal(int entityId) {
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
