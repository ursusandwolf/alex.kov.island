package com.island.view;

import com.island.model.Island;

/**
 * Interface for simulation visualization.
 * Allows decoupling the core engine from specific rendering implementations (Console, GUI, etc.).
 */
public interface SimulationView {
    /**
     * Renders the current state of the island.
     * @param island the island model to display
     */
    void display(Island island);

    /**
     * Enables or disables silent mode (no output).
     * @param silent true to suppress output
     */
    void setSilent(boolean silent);
}
