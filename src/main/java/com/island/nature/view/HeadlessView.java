package com.island.nature.view;

import com.island.engine.model.WorldSnapshot;

/**
 * A no-op implementation of SimulationView for headless mode.
 */
public class HeadlessView implements SimulationView {
    @Override
    public void display(WorldSnapshot snapshot) {
        // Do nothing
    }

    @Override
    public void setSilent(boolean silent) {
        // Do nothing
    }
}
