package com.island.engine.core;

import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;
import com.island.util.common.RandomProvider;

import java.util.concurrent.ExecutorService;

/**
 * Read-only container that holds all major components of a simulation instance.
 * Implements {@link AutoCloseable} to ensure proper resource cleanup (stopping the loop,
 * shutting down executors).
 *
 * <p>Always use this context within a try-with-resources block or ensure {@link #close()}
 * is called manually to prevent thread leaks.</p>
 *
 * @param world    The spatial and logical state of the simulation.
 * @param gameLoop The scheduling engine that drives simulation ticks.
 * @param random   The provider for stochastic operations (reproduction, movement, etc.).
 * @param eventBus The central communication channel for decoupled domain events.
 * @param executor The thread pool used for parallel task execution.
 * @param <T>      The base type of entities in the simulation.
 * @since 1.0
 */
@EngineAPI
public record SimulationContext<T extends Mortal>(
        SimulationWorld<T> world,
        GameLoop<T> gameLoop,
        RandomProvider random,
        EventBus eventBus,
        ExecutorService executor
) implements AutoCloseable {
    /**
     * Shuts down the simulation loop and the associated executor service.
     * Re-entrant safe.
     */
    @Override
    public void close() {
        gameLoop.stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}