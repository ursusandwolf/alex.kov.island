package com.island.engine;

/**
 * A simulation task that processes individual nodes in parallel.
 *
 * @param <T> The base type of entities in the nodes processed by this service.
 * @param <N> The type of simulation node processed by this service.
 */
public interface CellService<T extends Mortal, N extends SimulationNode<T>> extends ScheduledTask {
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

    /**
     * Optional setup phase called once per tick before parallel processing starts.
     */
    default void beforeTick(int tickCount) { }

    /**
     * Processes a single simulation node.
     */
    void processCell(N node, int tickCount);

    /**
     * Optional cleanup phase called once per tick after parallel processing finishes.
     */
    default void afterTick(int tickCount) { }

    /**
     * Implementation of Tickable that delegates to before/after.
     * When run via GameLoop's optimized path, this won't be called directly.
     */
    @Override
    default void tick(int tickCount) {
        // Fallback for non-optimized execution
        beforeTick(tickCount);
    }
}
