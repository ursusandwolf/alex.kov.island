package com.island.engine.core;

import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.island.engine.model.Mortal;
import com.island.engine.parallel.ParallelDispatcher;
import com.island.engine.scheduling.GameLoop;
import com.island.engine.scheduling.PhaseScheduler;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;

/**
 * Orchestrator that bootstraps a simulation using a plugin.
 */
public class SimulationEngine<T extends Mortal> {

    /**
     * Starts a simulation using the provided plugin and parameters.
     *
     * @param plugin         The plugin defining the simulation domain.
     * @param tickDurationMs Target duration for each tick.
     * @param threads        Number of threads for parallel execution.
     * @return The created simulation context.
     */
    public SimulationContext<T> start(SimulationPlugin<T> plugin, int tickDurationMs, int threads) {
        SimulationContext<T> context = build(plugin, tickDurationMs, threads);
        context.gameLoop().start();
        return context;
    }

    /**
     * Builds a simulation context using the provided plugin but DOES NOT start the loop.
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, int tickDurationMs, int threads) {
        EventBus eventBus = new DefaultEventBus();

        SimulationWorld<T> world = plugin.createWorld(eventBus);
        world.initialize();

        ExecutorService executor = (threads > 0)
                ? Executors.newFixedThreadPool(threads)
                : Executors.newVirtualThreadPerTaskExecutor();
        
        ParallelDispatcher<T> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<T> scheduler = new PhaseScheduler<>(dispatcher);

        GameLoop<T> gameLoop = new GameLoop<>(tickDurationMs, executor, scheduler);
        gameLoop.setWorld(world);

        plugin.registerTasks(gameLoop, world, eventBus);

        RandomProvider random = new DefaultRandomProvider();
        SimulationContext<T> context = new SimulationContext<>(world, gameLoop, random, eventBus);
        gameLoop.setStopCondition(() -> plugin.shouldStop(context));
        gameLoop.setOnStopCallback(() -> stop(context, plugin));

        plugin.onSimulationStarted(context);
        return context;
    }

    /**
     * Stops the simulation and notifies the plugin.
     *
     * @param context The simulation context to stop.
     * @param plugin  The plugin that defined the simulation.
     */
    public void stop(SimulationContext<T> context, SimulationPlugin<T> plugin) {
        context.gameLoop().stop();
        plugin.onSimulationStopped(context);
    }
}