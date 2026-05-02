package com.island.nature.view;

import com.island.engine.SimulationRenderer;
import com.island.engine.WorldSnapshot;

/**
 * Interface for simulation visualization.
 * Allows decoupling the core engine from specific rendering implementations (Console, GUI, etc.).
 */
public interface SimulationView extends SimulationRenderer {
}
