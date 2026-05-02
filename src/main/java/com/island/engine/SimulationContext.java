package com.island.engine;

import com.island.util.RandomProvider;

/**
 * Context that holds all major components of a running simulation.
 *
 * @param <T> The base type of entities in the simulation.
 */
public class SimulationContext<T extends Mortal> {
    private final SimulationWorld<T> world;
    private final GameLoop<T> gameLoop;
    private final SimulationRenderer view;
    private final RandomProvider random;

    public SimulationContext(SimulationWorld<T> world, GameLoop<T> gameLoop, 
                             SimulationRenderer view, RandomProvider random) {
        this.world = world;
        this.gameLoop = gameLoop;
        this.view = view;
        this.random = random;
    }

    public SimulationWorld<T> getWorld() {
        return world;
    }

    public GameLoop<T> getGameLoop() {
        return gameLoop;
    }

    public SimulationRenderer getView() {
        return view;
    }

    public RandomProvider getRandom() {
        return random;
    }
}
