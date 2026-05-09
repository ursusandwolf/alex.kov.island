package com.island.engine.core;

/**
 * Public API for high-performance health data storage.
 */
@EngineAPI
public interface HealthStorage {
    void set(int entityId, long currentEnergy, long maxEnergy, boolean alive);
    long getCurrentEnergy(int entityId);
    long getMaxEnergy(int entityId);
    boolean isAlive(int entityId);
    void setCurrentEnergy(int entityId, long energy);
    void setAlive(int entityId, boolean isAlive);

    static HealthStorage create(int initialCapacity) {
        return new com.island.engine.internal.HealthSoAStore(initialCapacity);
    }
}
