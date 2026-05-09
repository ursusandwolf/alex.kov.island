package com.island.engine.core;

import com.island.engine.event.EventBus;
import com.island.engine.internal.ParallelDispatcher;
import com.island.engine.internal.PhaseScheduler;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrator that bootstraps a simulation using a plugin.
 */
@EngineAPI
public class SimulationEngine<T extends Mortal> {

    /**
     * Starts a simulation using the provided plugin and parameters.
     *
     * @param plugin         The plugin defining the simulation domain.
     * @param config         Simulation configuration.
     * @return The created simulation context.
     */
    public SimulationContext<T> start(SimulationPlugin<T> plugin, SimulationConfig config) {
        SimulationContext<T> context = build(plugin, config);
        context.gameLoop().start();
        return context;
    }

    /**
     * Builds a simulation context using the provided plugin but DOES NOT start the loop.
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, SimulationConfig config) {
        EventBus eventBus = EventBus.create();

        SimulationWorld<T> world = plugin.createWorld(eventBus);
        world.initialize();

        ExecutorService executor = (config.getThreadCount() > 0)
                ? Executors.newFixedThreadPool(config.getThreadCount())
                : Executors.newVirtualThreadPerTaskExecutor();
        
        ParallelDispatcher<T> dispatcher = new ParallelDispatcher<>(executor);
        PhaseScheduler<T> scheduler = new PhaseScheduler<>(dispatcher);

        GameLoop<T> gameLoop = new GameLoop<>(config.getTickDurationMs(), executor, scheduler);
        gameLoop.setWorld(world);

        plugin.registerTasks(gameLoop, world, eventBus);

        RandomProvider random = new DefaultRandomProvider();
        SimulationContext<T> context = new SimulationContext<>(world, gameLoop, random, eventBus, executor);
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
        context.close();
        plugin.onSimulationStopped(context);
    }
}
