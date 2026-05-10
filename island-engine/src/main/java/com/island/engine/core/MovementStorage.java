package com.island.engine.core;

/**
 * Public API for high-performance movement data storage.
 */
@EngineAPI
public interface MovementStorage {
    void set(int entityId, int speed);
    int getSpeed(int entityId);
    void setSpeed(int entityId, int speed);

    static MovementStorage create(int initialCapacity) {
        return new com.island.engine.internal.MovementSoAStore(initialCapacity);
    }
}
