package com.island.engine.core;

import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;

/**
 * SPI (Service Provider Interface) for simulation domains.
 * Implementations define the world structure, entities, and execution logic (tasks).
 *
 * <p>Plugins are the primary way to extend the engine with specific simulation logic,
 * such as an island ecosystem or a city economy.</p>
 *
 * @param <T> The base type of entities (must implement {@link Mortal}).
 * @since 1.0
 */
@EngineAPI
public interface SimulationPlugin<T extends Mortal> {
    /**
     * Creates and initializes the simulation world.
     * This is called once during engine bootstrapping.
     *
     * @param eventBus the event bus for decoupled communication within the domain.
     * @return a fully initialized simulation world instance.
     */
    SimulationWorld<T> createWorld(EventBus eventBus);

    /**
     * Registers recurring tasks (ECS systems or services) to the game loop.
     * These tasks define the behavioral logic of the simulation.
     *
     * @param gameLoop the game loop where tasks should be registered.
     * @param world    the world instance created by {@link #createWorld(EventBus)}.
     * @param eventBus the event bus for decoupled communication.
     */
    void registerTasks(GameLoop<T> gameLoop, SimulationWorld<T> world, EventBus eventBus);

    /**
     * Hook called after the simulation context is fully built and ready to start.
     * Use this for final initialization or to subscribe domain services to the event bus.
     *
     * @param context the complete simulation context.
     */
    default void onSimulationStarted(SimulationContext<T> context) { }

    /**
     * Hook called after the simulation loop has terminated.
     * Use this for cleanup or to finalize statistics.
     *
     * @param context the complete simulation context.
     */
    default void onSimulationStopped(SimulationContext<T> context) { }

    /**
     * Evaluates whether the simulation has reached a termination condition.
     * This is checked at the end of every tick.
     *
     * @param context the complete simulation context.
     * @return {@code true} if the simulation should stop, {@code false} otherwise.
     */
    default boolean shouldStop(SimulationContext<T> context) {
        return false;
    }
}