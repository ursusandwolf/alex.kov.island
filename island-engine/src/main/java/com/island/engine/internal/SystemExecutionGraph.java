package com.island.engine.internal;

import com.island.engine.model.Mortal;
import com.island.engine.core.ParallelTask;
import com.island.engine.ecs.Component;
import com.island.engine.ecs.EntitySystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.island.engine.core.InternalEngine;

/**
 * Builds an execution schedule grouping independent systems into parallel phases.
 */
@InternalEngine
public final class SystemExecutionGraph {

    /**
     * Splits a list of parallel tasks into ordered batches.
     * Tasks in the same batch have no read/write conflicts and can be executed safely
     * across different nodes concurrently without cross-node race conditions.
     * 
     * @param tasks The tasks to schedule.
     * @param <T>   The entity type.
     * @return A list of batches (lists of tasks).
     */
    public static <T extends Mortal> List<List<ParallelTask<T>>> buildSchedule(List<ParallelTask<T>> tasks) {
        if (tasks.isEmpty()) {
            return List.of();
        }

        // Sort by priority first (descending)
        List<ParallelTask<T>> sorted = new ArrayList<>(tasks);
        sorted.sort((a, b) -> Integer.compare(b.priority(), a.priority()));

        List<List<ParallelTask<T>>> schedule = new ArrayList<>();

        for (ParallelTask<T> task : sorted) {
            boolean placed = false;
            // Try to place in the earliest possible batch without conflicts
            for (List<ParallelTask<T>> batch : schedule) {
                if (!conflictsWithAny(task, batch)) {
                    batch.add(task);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                List<ParallelTask<T>> newBatch = new ArrayList<>();
                newBatch.add(task);
                schedule.add(newBatch);
            }
        }

        return schedule;
    }

    private static <T extends Mortal> boolean conflictsWithAny(ParallelTask<T> task, List<ParallelTask<T>> batch) {
        for (int i = 0; i < batch.size(); i++) {
            if (conflicts(task, batch.get(i))) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Mortal> boolean conflicts(ParallelTask<T> a, ParallelTask<T> b) {
        if (a instanceof EntitySystem<?> sysA && b instanceof EntitySystem<?> sysB) {
            List<Class<? extends Component>> aRead = sysA.readComponents();
            List<Class<? extends Component>> aWrite = sysA.writeComponents();
            List<Class<? extends Component>> bRead = sysB.readComponents();
            List<Class<? extends Component>> bWrite = sysB.writeComponents();

            // Conflict occurs if:
            // A writes and B reads/writes
            // B writes and A reads/writes
            for (Class<? extends Component> c : aWrite) {
                if (bRead.contains(c) || bWrite.contains(c)) {
                    return true;
                }
            }
            
            for (Class<? extends Component> c : bWrite) {
                if (aRead.contains(c) || aWrite.contains(c)) {
                    return true;
                }
            }
            
            return false;
        }
        
        return a.conflictsWith(b) || b.conflictsWith(a);
    }
}
