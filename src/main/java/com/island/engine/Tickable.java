package com.island.engine;

/**
 * Interface for entities or services that perform actions every simulation tick.
 */
public interface Tickable {
    /**
     * Executes the logic for one simulation tick.
     * @param tickCount the current tick number
     */
    void tick(int tickCount);
}
