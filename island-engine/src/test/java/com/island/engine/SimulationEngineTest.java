package com.island.engine;

import com.island.engine.core.*;
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

    private static class TestMortal implements Mortal {
        @Override
        public boolean isAlive() { return true; }
        @Override
        public void die() {}
        @Override
        public String getTypeName() { return "Test"; }
    }
}
