package com.island.nature.entities.components;

import com.island.nature.model.Cell;

/**
 * Functional interface for consumption logic.
 *
 * @param <T> The type of context required for consumption.
 */
@FunctionalInterface
public interface ConsumeAction<T> {
    /**
     * Executes consumption logic.
     *
     * @param requestedAmount The amount of resource requested.
     * @param context The context for consumption (e.g. Cell, SimulationNode).
     * @return The actual amount consumed/gained.
     */
    long consume(long requestedAmount, T context);
}
