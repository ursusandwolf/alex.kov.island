package com.island.engine.core;

import com.island.engine.model.Mortal;
import com.island.engine.scheduling.ScheduledTask;

/**
 * A task that can be executed in parallel across multiple simulation nodes.
 *
 * @param <T> The base type of entities.
 */
@EngineAPI
public interface ParallelTask<T extends Mortal> extends ScheduledTask {
    /**
     * Optional setup phase called once per tick before parallel processing starts.
     */
    void beforeTick(int tickCount);

    /**
     * Processes a single simulation node.
     */
    void processCell(SimulationNode<T> node, int tickCount);

    /**
     * Optional cleanup phase called once per tick after parallel processing finishes.
     */
    void afterTick(int tickCount);

    /**
     * Checks if this task conflicts with another task.
     * If two tasks conflict, they cannot be executed in the same parallel batch.
     */
    default boolean conflictsWith(ParallelTask<T> other) {
        return true;
    }
    }