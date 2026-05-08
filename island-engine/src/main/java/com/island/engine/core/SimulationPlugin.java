package com.island.engine.core;

import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;

/**
 * Interface for simulation plugins that define specific worlds and tasks.
 *
 * @param <T> The base type of entities in the simulation.
 */
public interface SimulationPlugin<T extends Mortal> {
    /**
     * Creates and initializes the simulation world.
     *
     * @param eventBus the event bus for decoupled communication.
     */
    SimulationWorld<T> createWorld(EventBus eventBus);

    /**
     * Registers recurring tasks (services) to the game loop.
     *
     * @param gameLoop the game loop to register tasks to.
     * @param world    the world created by this plugin.
     * @param eventBus the event bus for decoupled communication.
     */
    void registerTasks(GameLoop<T> gameLoop, SimulationWorld<T> world, EventBus eventBus);

    /**
     * Optional hook called after the simulation context is created but before the loop starts.
     */
    default void onSimulationStarted(SimulationContext<T> context) { }

    /**
     * Optional hook called after the simulation loop stops.
     */
    default void onSimulationStopped(SimulationContext<T> context) { }

    /**
     * Optional hook to check if the simulation should stop based on its current state.
     */
    default boolean shouldStop(SimulationContext<T> context) {
        return false;
    }
}