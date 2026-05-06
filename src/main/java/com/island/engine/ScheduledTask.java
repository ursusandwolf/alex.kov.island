package com.island.engine;

public interface ScheduledTask extends Tickable {
    Phase phase();

    int priority();

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