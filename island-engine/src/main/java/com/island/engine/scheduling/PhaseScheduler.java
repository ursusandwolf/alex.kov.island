package com.island.engine.scheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import com.island.engine.core.InternalEngine;
import com.island.engine.core.SimulationWorld;
import com.island.engine.ecs.SystemExecutionGraph;
import com.island.engine.model.Mortal;
import com.island.engine.parallel.ParallelDispatcher;
import com.island.engine.parallel.ParallelTask;

/**
 * Manages the scheduling and sorting of simulation tasks across phases.
 *
 * @param <T> The base type of entities.
 */
@InternalEngine
@Slf4j
public class PhaseScheduler<T extends Mortal> {
    private static final Comparator<ScheduledTask> PRIORITY_COMPARATOR = 
            Comparator.comparingInt(ScheduledTask::priority).reversed();

    private final ParallelDispatcher<T> dispatcher;
    private final Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
    private final List<ParallelTask<T>> parallelGroup = new ArrayList<>();
    
    // Cache for optimized execution graph
    private long lastTasksVersion = -1;
    private final Map<Phase, List<List<ParallelTask<T>>>> cachedSchedules = new EnumMap<>(Phase.class);

    public PhaseScheduler(ParallelDispatcher<T> dispatcher) {
        this.dispatcher = dispatcher;
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
        }
    }

    public void execute(SimulationWorld<T> world, List<ScheduledTask> tasks, int tickCount, long tasksVersion) {
        if (lastTasksVersion != tasksVersion) {
            rebuildSchedule(tasks);
            lastTasksVersion = tasksVersion;
        }

        // Execute phases in order
        for (Phase phase : Phase.values()) {
            List<ScheduledTask> phaseTasks = phasedTasks.get(phase);
            if (phaseTasks.isEmpty()) {
                continue;
            }

            List<List<ParallelTask<T>>> batches = cachedSchedules.get(phase);
            if (batches != null) {
                // Optimized path: use cached parallel batches
                for (List<ParallelTask<T>> batch : batches) {
                    dispatcher.dispatch(world, batch, tickCount);
                }
                
                // Execute remaining sequential tasks in this phase (if any)
                for (ScheduledTask task : phaseTasks) {
                    if (task.asParallelTask() == null) {
                        executeSequential(task, phase, tickCount);
                    }
                }
            } else {
                // Fallback/Legacy path for phases without batches
                for (ScheduledTask task : phaseTasks) {
                    executeSequential(task, phase, tickCount);
                }
            }
        }
    }

    private void rebuildSchedule(List<ScheduledTask> tasks) {
        for (List<ScheduledTask> list : phasedTasks.values()) {
            list.clear();
        }
        cachedSchedules.clear();

        // Group and sort tasks
        for (ScheduledTask task : tasks) {
            phasedTasks.get(task.phase()).add(task);
        }
        for (Phase phase : Phase.values()) {
            List<ScheduledTask> phaseTasks = phasedTasks.get(phase);
            if (phaseTasks.isEmpty()) {
                continue;
            }

            phaseTasks.sort(PRIORITY_COMPARATOR);

            parallelGroup.clear();
            for (ScheduledTask task : phaseTasks) {
                ParallelTask<T> pt = task.asParallelTask();
                if (pt != null) {
                    parallelGroup.add(pt);
                }
            }

            if (!parallelGroup.isEmpty()) {
                cachedSchedules.put(phase, SystemExecutionGraph.buildSchedule(parallelGroup));
            }
        }
    }

    private void executeSequential(ScheduledTask task, Phase phase, int tickCount) {
        try {
            task.tick(tickCount);
        } catch (Exception e) {
            log.error("Error during simulation tick in phase {}: {}", phase, e.getMessage(), e);
        }
    }
}