package com.island.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the scheduling and sorting of simulation tasks across phases.
 *
 * @param <T> The base type of entities.
 */
public class PhaseScheduler<T extends Mortal> {
    private static final Logger log = LoggerFactory.getLogger(PhaseScheduler.class);
    private final Map<Phase, List<ScheduledTask>> phasedTasks = new EnumMap<>(Phase.class);
    private final List<CellService<T, SimulationNode<T>>> parallelGroup = new ArrayList<>();
    private final ParallelDispatcher<T> dispatcher;

    public PhaseScheduler(ParallelDispatcher<T> dispatcher) {
        this.dispatcher = dispatcher;
        for (Phase phase : Phase.values()) {
            phasedTasks.put(phase, new ArrayList<>());
        }
    }

    public void execute(SimulationWorld<T> world, List<ScheduledTask> tasks, int tickCount) {
        // Clear structures to reduce allocations
        for (List<ScheduledTask> list : phasedTasks.values()) {
            list.clear();
        }

        // Group tasks by phase
        for (ScheduledTask task : tasks) {
            phasedTasks.get(task.phase()).add(task);
        }

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
                if (task.executionMode() == ExecutionMode.PARALLEL && task instanceof CellService) {
                    @SuppressWarnings("unchecked")
                    CellService<T, SimulationNode<T>> cellService = (CellService<T, SimulationNode<T>>) task;
                    parallelGroup.add(cellService);
                } else {
                    // Dispatch any accumulated parallel services before sequential task
                    if (!parallelGroup.isEmpty()) {
                        dispatcher.dispatch(world, parallelGroup, tickCount);
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
                dispatcher.dispatch(world, parallelGroup, tickCount);
                parallelGroup.clear();
            }
        }
    }
}
