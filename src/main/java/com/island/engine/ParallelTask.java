package com.island.engine;

/**
 * A task that can be executed in parallel across multiple simulation nodes.
 *
 * @param <T> The base type of entities.
 */
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
}
