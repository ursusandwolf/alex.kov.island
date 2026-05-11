package com.island.engine.scheduling;

import com.island.engine.core.EngineAPI;

/**
 * Defines the logical phases of a single simulation tick.
 * Phases are executed sequentially in the order they are defined.
 */
@EngineAPI
public enum Phase {
    /**
     * Preparation phase for global systems (e.g., climate, global state updates).
     */
    PREPARE,

    /**
     * The main simulation phase where entities and domain systems are processed.
     */
    SIMULATION,

    /**
     * Post-processing phase for statistics collection, cleanup, and logging.
     */
    POSTPROCESS
}