package com.island.engine;

import com.island.engine.core.SimulationContext;
import com.island.engine.scheduling.GameLoop;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.*;

class SimulationContextTest {

    @Test
    @DisplayName("SimulationContext: Proper resource cleanup on close")
    void context_close_test() {
        GameLoop loop = mock(GameLoop.class);
        ExecutorService executor = mock(ExecutorService.class);
        
        SimulationContext context = new SimulationContext(null, loop, null, null, executor);
        context.close();
        
        verify(loop).stop();
        verify(executor).shutdown();
    }
}
