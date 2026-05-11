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
    @DisplayName("RandomUtils: All methods")
    void random_utils_full_test() {
        com.island.util.common.DefaultRandomProvider random = new com.island.util.common.DefaultRandomProvider(42L);
        RandomUtils.setProvider(random);
        
        assertTrue(RandomUtils.nextInt(10) < 10);
        assertTrue(RandomUtils.nextInt(10, 20) >= 10);
        assertTrue(RandomUtils.nextDouble() >= 0);
        assertTrue(RandomUtils.nextDouble(5.0) < 5.0);
        assertTrue(RandomUtils.checkChance(100));
    }

    @Test
    @DisplayName("ViewUtils: Sparkline edge cases")
    void view_utils_extended_test() {
        // Range 0
        assertEquals("▄", ViewUtils.getSparkline(List.of(1), 1));
        assertEquals(" ", ViewUtils.getSparkline(List.of(0), 1));
        
        // Data smaller than width
        String spark = ViewUtils.getSparkline(List.of(1, 10), 5);
        assertEquals(5, spark.length());
        assertTrue(spark.startsWith("   "));
        
        // Data larger than width
        String sparkLarge = ViewUtils.getSparkline(List.of(1, 2, 3, 4, 5), 2);
        assertEquals(2, sparkLarge.length());
    }
}
