package com.island.engine.core;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationConfig {
    private final int tickDurationMs;
    private final int threadCount;
    private final ExecutionMode executionMode;

    public static SimulationConfig defaultFor(int threadCount) {
        return SimulationConfig.builder()
                .tickDurationMs(100)
                .threadCount(threadCount)
                .executionMode(threadCount > 0 ? ExecutionMode.SEQUENTIAL : ExecutionMode.PARALLEL)
                .build();
    }
}
