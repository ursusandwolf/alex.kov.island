package com.island.engine.service;

import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationNode;
import com.island.engine.model.Mortal;
import com.island.engine.model.Tickable;
import com.island.engine.core.ParallelTask;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.Phase;

/**
 * A simulation task that processes individual nodes in parallel.
 *
 * @param <T> The base type of entities in the nodes processed by this service.
 */
public interface CellService<T extends Mortal> extends ParallelTask<T> {
    @Override
    default Phase phase() {
        return Phase.SIMULATION;
    }

    @Override
    default int priority() {
        return 50;
    }

    @Override
    default ExecutionMode executionMode() {
        return ExecutionMode.PARALLEL;
    }

    @Override
    @SuppressWarnings("unchecked")
    default <M extends Mortal> ParallelTask<M> asParallelTask() {
        return (ParallelTask<M>) this;
    }

    /**
     * Optional setup phase called once per tick before parallel processing starts.
     */
    @Override
    default void beforeTick(int tickCount) { }

    /**
     * Processes a single simulation node.
     */
    @Override
    void processCell(SimulationNode<T> node, int tickCount);

    /**
     * Optional cleanup phase called once per tick after parallel processing finishes.
     */
    @Override
    default void afterTick(int tickCount) { }

    /**
     * Implementation of Tickable that delegates to before/after.
     * When run via GameLoop's optimized path, this won't be called directly.
     */
    @Override
    default void tick(int tickCount) {
        throw new UnsupportedOperationException("CellService must be executed via GameLoop's optimized parallel path or provided with a world context for iteration.");
    }
}