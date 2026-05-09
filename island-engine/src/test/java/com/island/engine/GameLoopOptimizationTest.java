package com.island.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationNode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.core.DefaultWorkUnit;
import com.island.engine.model.Mortal;
import com.island.engine.internal.ParallelDispatcher;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.Phase;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.scheduling.ScheduledTask;

import com.island.engine.service.CellService;

class GameLoopOptimizationTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleParallelExecutionCorrectly() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(100, executor, scheduler);
        SimulationWorld<Mortal> world = mock(SimulationWorld.class);
        gameLoop.setWorld(world);

        // Create mock nodes and work units
        SimulationNode<Mortal> node = mock(SimulationNode.class);
        WorkUnit<Mortal> unit = new DefaultWorkUnit<>(Collections.singletonList(node));
        Collection<WorkUnit<Mortal>> workUnits = Collections.singletonList(unit);
        when(world.getParallelWorkUnits()).thenAnswer(inv -> workUnits);

        // Mock a parallel service
        CellService<Mortal> service = mock(CellService.class);
        when(service.executionMode()).thenReturn(ExecutionMode.PARALLEL);
        when(service.phase()).thenReturn(Phase.SIMULATION);
        when(service.priority()).thenReturn(50);
        when(service.asParallelTask()).thenReturn(service);
        gameLoop.addRecurringTask(service);

        // Run multiple ticks
        for (int i = 1; i <= 5; i++) {
            gameLoop.runTick();
            final int tick = i;
            verify(service, timeout(2000).atLeast(tick)).processCell(any(), anyInt());
        }

        // Verify that processorPool contains exactly 1 processor (since we have 1 work unit)
        java.lang.reflect.Field poolField = ParallelDispatcher.class.getDeclaredField("processorPool");
        poolField.setAccessible(true);
        List<?> pool = (List<?>) poolField.get(dispatcher);
        
        assertEquals(1, pool.size(), "Processor pool should contain exactly one reused processor");
        assertTrue(gameLoop.getTickCount() >= 5);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldHandleParallelErrorsGracefully() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        ParallelDispatcher<Mortal> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        GameLoop<Mortal> gameLoop = new GameLoop<>(100, executor, scheduler);
        SimulationWorld<Mortal> world = mock(SimulationWorld.class);
        gameLoop.setWorld(world);

        SimulationNode<Mortal> node = mock(SimulationNode.class);
        WorkUnit<Mortal> unit = new DefaultWorkUnit<>(Collections.singletonList(node));
        Collection<WorkUnit<Mortal>> workUnits = Collections.singletonList(unit);
        when(world.getParallelWorkUnits()).thenAnswer(inv -> workUnits);

        AtomicInteger callCount = new AtomicInteger(0);
        CellService<Mortal> service = mock(CellService.class);
        when(service.executionMode()).thenReturn(ExecutionMode.PARALLEL);
        when(service.phase()).thenReturn(Phase.SIMULATION);
        when(service.asParallelTask()).thenReturn(service);
        
        doAnswer(inv -> {
            callCount.incrementAndGet();
            throw new RuntimeException("Parallel boom");
        }).when(service).processCell(any(), anyInt());

        gameLoop.addRecurringTask(service);
        
        // This should not throw exception out of runTick
        assertDoesNotThrow(gameLoop::runTick);
        assertEquals(1, callCount.get());
    }
}