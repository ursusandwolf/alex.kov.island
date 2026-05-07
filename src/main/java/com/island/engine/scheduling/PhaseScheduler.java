package com.island.engine.scheduling;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.island.engine.core.SimulationWorld;
import com.island.engine.model.Mortal;
import com.island.engine.parallel.ParallelDispatcher;
import com.island.engine.parallel.ParallelTask;

/**
 * Manages the scheduling and sorting of simulation tasks across phases.
 *
 * @param <T> The base type of entities.
 */
@Slf4j
@RequiredArgsConstructor
public class PhaseScheduler<T extends Mortal> {
    private final ParallelDispatcher<T> dispatcher;

    public void execute(SimulationWorld<T> world, List<ScheduledTask> tasks, int tickCount) {
        Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
        }

        // Group tasks by phase
        for (ScheduledTask task : tasks) {
            phasedTasks.get(task.phase()).add(task);
        }

        List<ParallelTask<T>> parallelGroup = new ArrayList<>();

        // Execute phases in order
        for (Phase phase : Phase.values()) {
            List<ScheduledTask> phaseTasks = phasedTasks.get(phase);
            if (phaseTasks.isEmpty()) {
                continue;
            }

            // Sort descending by priority (higher priority executes first)
            phaseTasks.sort(Comparator.comparingInt(ScheduledTask::priority).reversed());

            parallelGroup.clear();
            for (ScheduledTask task : phaseTasks) {
                ParallelTask<T> parallelTask = task.asParallelTask();
                if (parallelTask != null) {
                    parallelGroup.add(parallelTask);
                } else {
                    // Dispatch any accumulated parallel services before sequential task
                    if (!parallelGroup.isEmpty()) {
                        List<List<ParallelTask<T>>> batches = com.island.engine.ecs.SystemExecutionGraph.buildSchedule(parallelGroup);
                        for (List<ParallelTask<T>> batch : batches) {
                            dispatcher.dispatch(world, batch, tickCount);
                        }
                        parallelGroup.clear();
                    }
                    try {
                        task.tick(tickCount);
                    } catch (Exception e) {
                        log.error("Error during simulation tick in phase {}: {}", phase, e.getMessage(), e);
                    }
                }
            }
            
            // Dispatch remaining parallel services
            if (!parallelGroup.isEmpty()) {
                List<List<ParallelTask<T>>> batches = com.island.engine.ecs.SystemExecutionGraph.buildSchedule(parallelGroup);
                for (List<ParallelTask<T>> batch : batches) {
                    dispatcher.dispatch(world, batch, tickCount);
                }
                parallelGroup.clear();
            }
        }
    }
}