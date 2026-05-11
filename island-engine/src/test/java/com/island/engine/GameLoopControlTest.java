package com.island.engine;

import com.island.engine.core.SimulationWorld;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.SimulationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GameLoopControlTest {

    @Test
    @DisplayName("GameLoop: Pause and Resume")
    void pause_resume_test() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        PhaseScheduler scheduler = mock(PhaseScheduler.class);
        SimulationWorld world = mock(SimulationWorld.class);
        
        GameLoop loop = new GameLoop(10, executor, scheduler);
        loop.setWorld(world);
        
        assertEquals(SimulationStatus.IDLE, loop.getStatus());
        
        loop.start();
        assertTrue(loop.isRunning());
        assertEquals(SimulationStatus.RUNNING, loop.getStatus());
        
        loop.pause();
        assertEquals(SimulationStatus.PAUSED, loop.getStatus());
        
        int countAfterPause = loop.getTickCount();
        Thread.sleep(50);
        // Should not have incremented much (maybe one tick if it was already running)
        assertTrue(loop.getTickCount() <= countAfterPause + 1);
        
        loop.resume();
        assertEquals(SimulationStatus.RUNNING, loop.getStatus());
        
        Thread.sleep(50);
        assertTrue(loop.getTickCount() > countAfterPause);
        
        loop.stop();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}
