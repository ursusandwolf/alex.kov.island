package com.island.engine;

import com.island.engine.core.ParallelTask;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.EntitySystem;
import com.island.engine.internal.SystemExecutionGraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemExecutionGraphTest {

    @Test
    @DisplayName("SystemExecutionGraph: Independent tasks in same batch")
    void independent_tasks_test() {
        EntitySystem s1 = mock(EntitySystem.class);
        when(s1.readComponents()).thenReturn(Collections.emptyList());
        when(s1.writeComponents()).thenReturn(Collections.emptyList());
        when(s1.priority()).thenReturn(100);

        EntitySystem s2 = mock(EntitySystem.class);
        when(s2.readComponents()).thenReturn(Collections.emptyList());
        when(s2.writeComponents()).thenReturn(Collections.emptyList());
        when(s2.priority()).thenReturn(50);

        List<List<ParallelTask>> batches = SystemExecutionGraph.buildSchedule(List.of(s1, s2));
        assertEquals(1, batches.size());
        assertEquals(2, batches.get(0).size());
    }

    @Test
    @DisplayName("SystemExecutionGraph: Conflict between systems")
    void conflict_tasks_test() {
        class C1 implements Component {}
        
        EntitySystem s1 = mock(EntitySystem.class);
        when(s1.writeComponents()).thenReturn(List.of(C1.class));
        when(s1.readComponents()).thenReturn(Collections.emptyList());
        when(s1.priority()).thenReturn(100);

        EntitySystem s2 = mock(EntitySystem.class);
        when(s2.readComponents()).thenReturn(List.of(C1.class));
        when(s2.writeComponents()).thenReturn(Collections.emptyList());
        when(s2.priority()).thenReturn(50);

        List<List<ParallelTask>> batches = SystemExecutionGraph.buildSchedule(List.of(s1, s2));
        assertEquals(2, batches.size());
    }

    @Test
    @DisplayName("SystemExecutionGraph: Custom conflict logic")
    void custom_conflict_test() {
        ParallelTask t1 = mock(ParallelTask.class);
        when(t1.priority()).thenReturn(100);
        ParallelTask t2 = mock(ParallelTask.class);
        when(t2.priority()).thenReturn(50);
        
        when(t1.conflictsWith(t2)).thenReturn(true);
        
        List<List<ParallelTask>> batches = SystemExecutionGraph.buildSchedule(List.of(t1, t2));
        assertEquals(2, batches.size());
    }
}
