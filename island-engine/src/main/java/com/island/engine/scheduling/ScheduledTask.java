package com.island.engine.scheduling;

import com.island.engine.core.EngineAPI;
import com.island.engine.core.ExecutionMode;
import com.island.engine.model.Mortal;
import com.island.engine.model.Tickable;
import com.island.engine.core.ParallelTask;

/**
 * A task that can be scheduled for execution by the GameLoop.
 * Scheduled tasks have a phase and priority that determine their execution order.
 */
@EngineAPI
public interface ScheduledTask extends Tickable {
    /**
     * Gets the simulation phase this task belongs to.
     */
    Phase phase();

    /**
     * Gets the execution priority within the phase (higher values executed first).
     */
    int priority();

    /**
     * Gets the execution mode (SEQUENTIAL or PARALLEL).
     */
    default ExecutionMode executionMode() {
        return ExecutionMode.SEQUENTIAL;
    }

    /**
     * Converts this task to a ParallelTask if it supports parallel execution.
     *
     * @param <T> The base type of entities.
     * @return The ParallelTask or null if not supported.
     */
    default <T extends Mortal> ParallelTask<T> asParallelTask() {
        return null;
    }
}