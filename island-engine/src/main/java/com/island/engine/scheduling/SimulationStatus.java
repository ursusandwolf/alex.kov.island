package com.island.engine.scheduling;

import com.island.engine.core.EngineAPI;

/**
 * Represents the current state of the simulation loop.
 */
@EngineAPI
public enum SimulationStatus {
    /**
     * Simulation is not running.
     */
    IDLE, 
    
    /**
     * Simulation is active and ticking.
     */
    RUNNING, 
    
    /**
     * Simulation is active but ticks are suspended.
     */
    PAUSED
}
