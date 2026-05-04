package com.island.engine;

import com.island.engine.event.DefaultEventBus;
import com.island.engine.event.EventBus;
import com.island.util.DefaultRandomProvider;
import com.island.util.RandomProvider;

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
        context.getGameLoop().start();
        return context;
    }

    /**
     * Builds a simulation context using the provided plugin but DOES NOT start the loop.
     */
    public SimulationContext<T> build(SimulationPlugin<T> plugin, int tickDurationMs, int threads) {
        RandomProvider random = new DefaultRandomProvider();
        EventBus eventBus = new DefaultEventBus();

        SimulationWorld<T, ?> world = plugin.createWorld();
        world.setEventBus(eventBus);
        world.initialize();

        GameLoop<T> gameLoop = new GameLoop<>(tickDurationMs, threads);
        gameLoop.setWorld(world);

        plugin.registerTasks(gameLoop, world, eventBus);

        SimulationContext<T> context = new SimulationContext<>(world, gameLoop, random, eventBus);

        plugin.onSimulationStarted(context);
        return context;
    }
}
