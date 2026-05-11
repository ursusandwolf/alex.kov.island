package com.island.engine;

import com.island.engine.core.ParallelTask;
import com.island.engine.core.SimulationWorld;
import com.island.engine.core.WorkUnit;
import com.island.engine.internal.ParallelDispatcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

class ParallelDispatcherTest {

    @Test
    @DisplayName("ParallelDispatcher: Lifecycle and execution")
    void dispatcher_test() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ParallelDispatcher dispatcher = new ParallelDispatcher(executor);
        
        SimulationWorld world = mock(SimulationWorld.class);
        WorkUnit unit = mock(WorkUnit.class);
        when(world.getParallelWorkUnits()).thenReturn(Collections.singletonList(unit));
        when(unit.iterator()).thenReturn(Collections.emptyIterator());
        
        ParallelTask service = mock(ParallelTask.class);
        List<ParallelTask> services = Collections.singletonList(service);
        
        dispatcher.dispatch(world, services, 1);
        
        verify(service).beforeTick(1);
        verify(service).afterTick(1);
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("ParallelDispatcher: Service exception handling")
    void dispatcher_exception_test() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        ParallelDispatcher dispatcher = new ParallelDispatcher(executor);
        
        SimulationWorld world = mock(SimulationWorld.class);
        WorkUnit unit = mock(WorkUnit.class);
        when(world.getParallelWorkUnits()).thenReturn(Collections.singletonList(unit));
        when(unit.iterator()).thenReturn(Collections.emptyIterator());
        
        ParallelTask service = mock(ParallelTask.class);
        doThrow(new RuntimeException("Fail")).when(service).beforeTick(anyInt());
        
        dispatcher.dispatch(world, Collections.singletonList(service), 1);
        
        verify(service).beforeTick(1);
        // Dispatcher should continue despite exception in beforeTick
        verify(service).afterTick(1);
        
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.SECONDS);
    }
}
