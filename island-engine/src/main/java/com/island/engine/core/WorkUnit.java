package com.island.engine.core;

import java.util.Collection;
import com.island.engine.model.Mortal;

/**
 * Represents a group of {@link SimulationNode}s that can be processed 
 * as a single unit, typically in parallel.
 * 
 * <p>WorkUnits are used by the engine to distribute the simulation load across multiple 
 * threads. By implementing {@link Collection}, they allow easy iteration over their 
 * assigned nodes.</p>
 * 
 * <p>Execution time monitoring allows the world implementation to rebalance WorkUnits 
 * if some areas of the simulation become more computationally expensive than others.</p>
 * 
 * @param <T> The base type of entities in the simulation world.
 * @since 1.0
 */
@EngineAPI
public interface WorkUnit<T extends Mortal> extends Collection<SimulationNode<T>> {
    /**
     * Records the total wall-clock time taken to process all nodes in this unit 
     * during the current tick.
     * 
     * @param nanos time in nanoseconds.
     */
    void setLastExecutionTimeNanos(long nanos);

    /**
     * Retrieves the last recorded processing time. 
     * Used for load balancing and performance diagnostics.
     */
    long getLastExecutionTimeNanos();
}
