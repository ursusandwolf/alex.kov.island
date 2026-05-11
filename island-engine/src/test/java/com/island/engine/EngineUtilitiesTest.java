package com.island.engine;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.ExecutionMode;
import com.island.util.common.RandomUtils;
import com.island.util.math.GridUtils;
import com.island.util.common.ViewUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EngineUtilitiesTest {

    @Test
    @DisplayName("SimulationConfig: Default values and validation")
    void simulation_config_test() {
        SimulationConfig config = SimulationConfig.defaultFor(4);
        assertEquals(4, config.getThreadCount());
        assertEquals(ExecutionMode.PARALLEL, config.getExecutionMode());
        
        SimulationConfig sequential = SimulationConfig.defaultFor(0);
        assertEquals(ExecutionMode.SEQUENTIAL, sequential.getExecutionMode());
    }

    @Test
    @DisplayName("GridUtils: Coordinate validation")
    void grid_utils_test() {
        assertTrue(GridUtils.isValid(0, 0, 10, 10));
        assertTrue(GridUtils.isValid(9, 9, 10, 10));
        assertFalse(GridUtils.isValid(-1, 0, 10, 10));
        assertFalse(GridUtils.isValid(10, 10, 10, 10));
    }

    @Test
    @DisplayName("RandomUtils: Probability checks")
    void random_utils_test() {
        com.island.util.common.DefaultRandomProvider random = new com.island.util.common.DefaultRandomProvider(42L);
        RandomUtils.setProvider(random);
        
        // With seed 42, check some values if needed, but checkChance 100 is always true
        assertTrue(RandomUtils.checkChance(100));
        assertFalse(RandomUtils.checkChance(0));
    }

    @Test
    @DisplayName("ViewUtils: Sparkline generation")
    void view_utils_test() {
        List<Integer> data = List.of(1, 2, 3, 4, 5);
        String spark = ViewUtils.getSparkline(data, 10);
        assertEquals(10, spark.length());
        assertFalse(spark.trim().isEmpty());
        
        // Edge cases
        assertEquals(" ".repeat(10), ViewUtils.getSparkline(null, 10));
        assertEquals(" ".repeat(10), ViewUtils.getSparkline(List.of(), 10));
        
        String flat = ViewUtils.getSparkline(List.of(1, 1, 1), 3);
        assertEquals(3, flat.length());
    }
}
