package com.island.engine.core;

/**
 * Public API for managing unique entity IDs.
 */
@EngineAPI
public interface EntityIdProvider {
    int acquireId();
    void releaseId(int id);

    static EntityIdProvider create() {
        return new com.island.engine.internal.EntityIdManager();
    }
}
