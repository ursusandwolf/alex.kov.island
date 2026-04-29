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

/**
 * Base class for all simulation services.
 */
public abstract class AbstractService implements Tickable {
    private final SimulationWorld world;
    private final ExecutorService executor;
    private final RandomProvider random;

    protected AbstractService(SimulationWorld world, ExecutorService executor, RandomProvider random) {
        this.world = world;
        this.executor = executor;
        this.random = random;
    }

    public SimulationWorld getWorld() {
        return world;
    }

    protected RandomProvider getRandom() {
        return random;
    }

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

    protected abstract void processCell(SimulationNode node, int tickCount);
}
