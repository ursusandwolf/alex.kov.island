package com.island.engine.ecs;

import com.island.engine.model.Mortal;
import com.island.engine.parallel.ParallelTask;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds an execution schedule grouping independent systems into parallel phases.
 */
public class SystemExecutionGraph {

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
        for (ParallelTask<T> batchTask : batch) {
            if (conflicts(task, batchTask)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Mortal> boolean conflicts(ParallelTask<T> a, ParallelTask<T> b) {
        if (!(a instanceof EntitySystem) || !(b instanceof EntitySystem)) {
            // If we don't know the components, assume they conflict to be safe
            return true;
        }
        
        EntitySystem<?> sysA = (EntitySystem<?>) a;
        EntitySystem<?> sysB = (EntitySystem<?>) b;

        Set<Class<? extends Component>> aRead = new HashSet<>(sysA.readComponents());
        Set<Class<? extends Component>> aWrite = new HashSet<>(sysA.writeComponents());
        Set<Class<? extends Component>> bRead = new HashSet<>(sysB.readComponents());
        Set<Class<? extends Component>> bWrite = new HashSet<>(sysB.writeComponents());

        // Conflict occurs if:
        // A writes and B reads
        // A reads and B writes
        // A writes and B writes
        for (Class<? extends Component> c : aWrite) {
            if (bRead.contains(c) || bWrite.contains(c)) {
                return true;
            }
        }
        for (Class<? extends Component> c : aRead) {
            if (bWrite.contains(c)) {
                return true;
            }
        }
        return false;
    }
}
