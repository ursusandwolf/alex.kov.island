package com.island.engine.model;

import com.island.engine.core.EngineAPI;

/**
 * Interface for entities or services that perform actions every simulation tick.
 */
@EngineAPI
public interface Tickable {
    /**
     * Executes the logic for one simulation tick.
     * @param tickCount the current tick number
     */
    void tick(int tickCount);
}