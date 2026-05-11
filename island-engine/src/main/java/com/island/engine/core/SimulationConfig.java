package com.island.engine.core;

import lombok.Builder;
import lombok.Getter;

/**
 * Configuration parameters for a simulation instance.
 * Defines tick duration, threading model, and execution strategy.
 * 
 * <p>Use the provided {@link Builder} or the {@link #defaultFor(int)} factory method 
 * to create instances.</p>
 * 
 * @since 1.0
 */
@EngineAPI
@Getter
@Builder
public class SimulationConfig {
    /**
     * The target duration of a single simulation tick in milliseconds.
     * Default is 100ms.
     */
    private final int tickDurationMs;

    /**
     * The number of threads to use for parallel processing.
     * Use 0 for sequential execution or to use Java 21 Virtual Threads (if supported by environment).
     */
    private final int threadCount;

    /**
     * The execution strategy (SEQUENTIAL or PARALLEL).
     */
    private final ExecutionMode executionMode;

    /**
     * Creates a default configuration for the given number of threads.
     * 
     * @param threadCount the number of worker threads.
     * @return a SimulationConfig with 100ms ticks and the appropriate execution mode.
     */
    public static SimulationConfig defaultFor(int threadCount) {
        return SimulationConfig.builder()
                .tickDurationMs(100)
                .threadCount(threadCount)
                .executionMode(threadCount > 0 ? ExecutionMode.PARALLEL : ExecutionMode.SEQUENTIAL)
                .build();
    }
}
