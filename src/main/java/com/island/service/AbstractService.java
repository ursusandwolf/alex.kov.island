package com.island.service;

import com.island.engine.SimulationNode;
import com.island.engine.SimulationWorld;
import com.island.engine.Tickable;
import com.island.util.RandomProvider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Base class for all simulation services.
 */
@Getter
@RequiredArgsConstructor
public abstract class AbstractService implements Tickable {
    private final SimulationWorld world;
    private final ExecutorService executor;
    private final RandomProvider random;

    @Override
    public void tick(int tickCount) {
        if (executor.isShutdown()) {
            return;
        }

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Collection<? extends SimulationNode> workUnit : world.getParallelWorkUnits()) {
            tasks.add(() -> {
                for (SimulationNode node : workUnit) {
                    processCell(node, tickCount);
                }
                return null;
            });
        }
        try {
            if (!executor.isShutdown()) {
                executor.invokeAll(tasks);
            }
        } catch (java.util.concurrent.RejectedExecutionException e) {
            // Silently ignore if shutdown happened between check and execution
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Applies an action to a sampled subset of a list to maintain Level of Detail (LOD).
     */
    protected <T> void forEachSampled(List<T> list, int limit, Consumer<T> action) {
        int size = list.size();
        if (size == 0) {
            return;
        }
        int step = (size > limit) ? (size / limit + 1) : 1;
        for (int i = 0; i < size; i += step) {
            action.accept(list.get(i));
        }
    }

    protected abstract void processCell(SimulationNode node, int tickCount);
}
