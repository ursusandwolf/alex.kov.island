package com.island.engine;

/**
 * Interface for simulation visualization.
 * Decouples the core engine from specific rendering implementations.
 */
public interface SimulationRenderer {
    /**
     * Renders the current state of the simulation.
     * @param snapshot the world snapshot to display
     */
    void display(WorldSnapshot snapshot);

    /**
     * Enables or disables silent mode (no output).
     * @param silent true to suppress output
     */
    void setSilent(boolean silent);
}