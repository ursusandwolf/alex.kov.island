package com.island.engine.core;

/**
 * Public API for high-performance age data storage.
 */
@EngineAPI
public interface AgeStorage {
    void set(int entityId, int age, int maxLifespan);
    int getAge(int entityId);
    int getMaxLifespan(int entityId);
    void setAge(int entityId, int age);
    void setMaxLifespan(int entityId, int maxLifespan);

    static AgeStorage create(int initialCapacity) {
        return new com.island.engine.internal.AgeSoAStore(initialCapacity);
    }
}
