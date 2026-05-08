package com.island.engine.core;

import com.island.engine.event.EventBus;
import com.island.engine.model.Mortal;
import com.island.engine.scheduling.GameLoop;
import com.island.util.common.RandomProvider;

/**
 * Context that holds all major components of a running simulation.
 *
 * @param <T> The base type of entities in the simulation.
 */
@EngineAPI
public record SimulationContext<T extends Mortal>(
        SimulationWorld<T> world,
        GameLoop<T> gameLoop,
        RandomProvider random,
        EventBus eventBus
) {
}