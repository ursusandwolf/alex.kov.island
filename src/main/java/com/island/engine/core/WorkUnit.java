package com.island.engine.core;

import java.util.Collection;
import com.island.engine.model.Mortal;

/**
 * Represents a unit of work that can be processed in parallel.
 * Supports execution time monitoring for load balancing.
 */
public interface WorkUnit<T extends Mortal> extends Collection<SimulationNode<T>> {
    /**
     * Records the time taken to process this work unit.
     */
    void setLastExecutionTimeNanos(long nanos);

    /**
     * Gets the last recorded execution time.
     */
    long getLastExecutionTimeNanos();
}
