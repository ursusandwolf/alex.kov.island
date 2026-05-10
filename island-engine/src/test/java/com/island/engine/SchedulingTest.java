package com.island.engine;

import com.island.engine.core.ExecutionMode;
import com.island.engine.core.SimulationWorld;
import com.island.engine.internal.ParallelDispatcher;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SchedulingTest {

    @Test
    @DisplayName("PhaseScheduler: Task ordering and execution")
    void scheduler_test() {
        ParallelDispatcher<Mortal> dispatcher = mock(ParallelDispatcher.class);
        PhaseScheduler<Mortal> scheduler = new PhaseScheduler<>(dispatcher);
        SimulationWorld<Mortal> world = mock(SimulationWorld.class);
        
        ScheduledTask task1 = mock(ScheduledTask.class);
        when(task1.phase()).thenReturn(Phase.SIMULATION);
        when(task1.executionMode()).thenReturn(ExecutionMode.SEQUENTIAL);
        
        scheduler.execute(world, List.of(task1), 1, 1L);
        
        verify(task1).tick(1);
    }
    
    @Test
    @DisplayName("ScheduledTask: Default implementations")
    void task_default_test() {
        ScheduledTask task = new ScheduledTask() {
            @Override
            public void tick(int tickCount) {}
            @Override
            public Phase phase() { return Phase.SIMULATION; }
            @Override
            public int priority() { return 50; }
        };
        
        assertEquals(Phase.SIMULATION, task.phase());
        assertEquals(50, task.priority());
        assertEquals(ExecutionMode.SEQUENTIAL, task.executionMode());
    }
}
