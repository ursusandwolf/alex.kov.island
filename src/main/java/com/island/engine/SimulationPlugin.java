package com.island.engine;

/**
 * Interface for simulation plugins that define specific worlds and tasks.
 *
 * @param <T> The base type of entities in the simulation.
 */
public interface SimulationPlugin<T extends Mortal> {
    /**
     * Creates and initializes the simulation world.
     */
    SimulationWorld<T> createWorld();

    /**
     * Registers recurring tasks (services) to the game loop.
     *
     * @param gameLoop the game loop to register tasks to.
     * @param world    the world created by this plugin.
     */
    void registerTasks(GameLoop<T> gameLoop, SimulationWorld<T> world);

    /**
     * Optional hook called after the simulation context is created but before the loop starts.
     */
    default void onSimulationStarted(SimulationContext<T> context) { }

    /**
     * Optional hook called after the simulation loop stops.
     */
    default void onSimulationStopped(SimulationContext<T> context) { }
}
