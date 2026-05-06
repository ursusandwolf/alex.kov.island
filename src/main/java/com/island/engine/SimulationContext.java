package com.island.engine;

import com.island.engine.event.EventBus;
import com.island.util.RandomProvider;

/**
 * Context that holds all major components of a running simulation.
 *
 * @param <T> The base type of entities in the simulation.
 */
public record SimulationContext<T extends Mortal>(
        SimulationWorld<T> world,
        GameLoop<T> gameLoop,
        RandomProvider random,
        EventBus eventBus
) {
}
