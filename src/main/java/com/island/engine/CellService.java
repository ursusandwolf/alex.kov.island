package com.island.engine;

/**
 * A simulation task that processes individual nodes in parallel.
 */
public interface CellService extends Tickable {
    /**
     * Optional setup phase called once per tick before parallel processing starts.
     */
    default void beforeTick(int tickCount) { }

    /**
     * Processes a single simulation node.
     */
    void processCell(SimulationNode node, int tickCount);

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
