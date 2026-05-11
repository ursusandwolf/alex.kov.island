package com.island.engine;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimulationEngineTest {

    @Test
    @DisplayName("SimulationEngine: Build and lifecycle")
    void engine_build_test() {
        SimulationEngine<TestMortal> engine = new SimulationEngine<>();
        SimulationPlugin<TestMortal> plugin = mock(SimulationPlugin.class);
        SimulationWorld<TestMortal> world = mock(SimulationWorld.class);
        
        when(plugin.createWorld(any())).thenReturn(world);
        // registerTasks is void, nothing to stub
        
        SimulationConfig config = SimulationConfig.defaultFor(2);
        
        try (SimulationContext<TestMortal> context = engine.start(plugin, config)) {
            assertNotNull(context);
            assertEquals(world, context.world());
            assertNotNull(context.gameLoop());
            assertTrue(context.gameLoop().isRunning());
        }
    }

    @Test
    @DisplayName("SimulationEngine: Build with 3-arg overload")
    void engine_build_3arg_test() {
        SimulationEngine<TestMortal> engine = new SimulationEngine<>();
        SimulationPlugin<TestMortal> plugin = mock(SimulationPlugin.class);
        SimulationWorld<TestMortal> world = mock(SimulationWorld.class);
        when(plugin.createWorld(any())).thenReturn(world);
        
        try (SimulationContext<TestMortal> context = engine.build(plugin, 100, 4)) {
            assertNotNull(context);
            assertEquals(100, context.gameLoop().getTickDurationMs());
        }
    }

    @Test
    @DisplayName("SimulationEngine: Error on null world")
    void engine_null_world_test() {
        SimulationEngine<TestMortal> engine = new SimulationEngine<>();
        SimulationPlugin<TestMortal> plugin = mock(SimulationPlugin.class);
        when(plugin.createWorld(any())).thenReturn(null);
        
        assertThrows(IllegalArgumentException.class, () -> engine.build(plugin, SimulationConfig.defaultFor(1)));
    }

    private static class TestMortal implements Mortal {
        @Override
        public boolean isAlive() { return true; }
        @Override
        public void die() {}
        @Override
        public String getTypeName() { return "Test"; }
    }
}
