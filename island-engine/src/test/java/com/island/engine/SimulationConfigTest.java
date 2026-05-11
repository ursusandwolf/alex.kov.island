package com.island.engine;

import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimulationConfigTest {

    @Test
    @DisplayName("SimulationConfig: Default factory")
    void default_config_test() {
        SimulationConfig config = SimulationConfig.defaultFor(4);
        assertEquals(100, config.getTickDurationMs());
        assertEquals(4, config.getThreadCount());
        assertEquals(ExecutionMode.PARALLEL, config.getExecutionMode());
        
        SimulationConfig seqConfig = SimulationConfig.defaultFor(0);
        assertEquals(ExecutionMode.SEQUENTIAL, seqConfig.getExecutionMode());
    }

    @Test
    @DisplayName("SimulationConfig: Custom builder")
    void builder_test() {
        SimulationConfig config = SimulationConfig.builder()
                .tickDurationMs(50)
                .threadCount(8)
                .executionMode(ExecutionMode.PARALLEL)
                .build();
        assertEquals(50, config.getTickDurationMs());
        assertEquals(8, config.getThreadCount());
    }
}
