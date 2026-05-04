package com.island.engine;

/**
 * Defines how a task should be executed by the GameLoop.
 */
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
