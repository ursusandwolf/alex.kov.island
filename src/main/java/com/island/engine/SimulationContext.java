package com.island.engine;

import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.util.RandomProvider;
import com.island.view.SimulationView;

/**
 * Context that holds all major components of a running simulation.
 */
public class SimulationContext {
    private final Island island;
    private final GameLoop gameLoop;
    private final SpeciesRegistry speciesRegistry;
    private final SimulationView view;
    private final RandomProvider random;

    public SimulationContext(Island island, GameLoop gameLoop, SpeciesRegistry speciesRegistry, 
                             SimulationView view, RandomProvider random) {
        this.island = island;
        this.gameLoop = gameLoop;
        this.speciesRegistry = speciesRegistry;
        this.view = view;
        this.random = random;
    }

    public Island getIsland() {
        return island;
    }

    public GameLoop getGameLoop() {
        return gameLoop;
    }

    public SpeciesRegistry getSpeciesRegistry() {
        return speciesRegistry;
    }

    public SimulationView getView() {
        return view;
    }

    public RandomProvider getRandom() {
        return random;
    }

    @Deprecated
    public SimulationView getConsoleView() {
        return view;
    }
}
