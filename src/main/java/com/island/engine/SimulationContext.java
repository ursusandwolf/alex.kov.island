package com.island.engine;

import com.island.util.RandomProvider;
import lombok.Getter;

/**
 * Context that holds all major components of a running simulation.
 *
 * @param <T> The base type of entities in the simulation.
 */
@Getter
public class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T, ?> world;
    private final GameLoop<T> gameLoop;
    private final RandomProvider random;

    public SimulationContext(SimulationWorld<T, ?> world, GameLoop<T> gameLoop, RandomProvider random) {
        this.world = world;
        this.gameLoop = gameLoop;
        this.random = random;
    }
}
