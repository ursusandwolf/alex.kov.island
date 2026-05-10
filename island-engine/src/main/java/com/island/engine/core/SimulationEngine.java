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
 * Entry point for creating and managing simulation instances.
 * Orchestrates the bootstrapping process using a plugin and configuration.
 *
 * <p>Usage:
 * <pre>{@code
 * SimulationConfig config = SimulationConfig.defaultFor(4);
 * try (SimulationContext<Organism> ctx = new SimulationEngine<Organism>().build(plugin, config)) {
 *     ctx.gameLoop().start();
 *     // ...
 * }
 * }</pre>
 *
 * @param <T> the base entity type of the simulation
 * @since 1.0
 */
@EngineAPI
public class SimulationEngine<T extends Mortal> {

    /**
     * Starts a simulation using the provided plugin and parameters.
     * This method builds the context and immediately starts the game loop.
     *
     * @param plugin The plugin defining the simulation domain.
     * @param config Simulation configuration.
     * @return The created and started simulation context.
     */
    public SimulationContext<T> start(SimulationPlugin<T> plugin, SimulationConfig config) {
        SimulationContext<T> context = build(plugin, config);
        context.gameLoop().start();
        return context;
    }

    /**
     * Builds a simulation context from the given plugin and config.
     * The returned context must be closed when no longer needed (e.g. via try-with-resources).
     * This method does NOT start the game loop.
     *
     * @param plugin the simulation plugin providing world and tasks
     * @param config execution configuration (tick rate, thread count)
     * @return a ready-to-use simulation context
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, SimulationConfig config) {
        EventBus eventBus = EventBus.create();

        SimulationWorld<T> world = plugin.createWorld(eventBus);
        if (world == null) {
            throw new IllegalArgumentException("Plugin created a null world");
        }
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
     * Convenient overload for building a simulation context with basic parameters.
     *
     * @param plugin         The plugin defining the simulation domain.
     * @param tickDurationMs The duration of a single simulation tick in milliseconds.
     * @param threadCount    The number of threads to use for parallel execution.
     * @return a ready-to-use simulation context
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, int tickDurationMs, int threadCount) {
        return build(plugin, SimulationConfig.builder()
                .tickDurationMs(tickDurationMs)
                .threadCount(threadCount)
                .executionMode(threadCount > 0 ? ExecutionMode.PARALLEL : ExecutionMode.SEQUENTIAL)
                .build());
    }

    /**
     * Stops the simulation and notifies the plugin.
     * Also closes the context, releasing associated resources (executor, etc.).
     *
     * @param context The simulation context to stop.
     * @param plugin  The plugin that defined the simulation.
     */
    public void stop(SimulationContext<T> context, SimulationPlugin<T> plugin) {
        context.close();
        plugin.onSimulationStopped(context);
    }
}
