package com.island.engine.core;

import com.island.engine.scheduling.GameLoop;

/**
 * Defines how a task should be executed by the GameLoop.
 */
@EngineAPI
public enum ExecutionMode {
    /**
     * Executed sequentially in the main loop thread.
     */
    SEQUENTIAL,

    /**
     * Executed in parallel using available workers.
     * Parallelizable tasks must be thread-safe.
     */
    PARALLEL
}