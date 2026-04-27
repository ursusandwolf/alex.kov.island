package com.island.engine;

import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.view.ConsoleView;

/**
 * Context that holds all major components of a running simulation.
 */
public class SimulationContext {
    private final Island island;
    private final GameLoop gameLoop;
    private final SpeciesRegistry speciesRegistry;
    private final ConsoleView consoleView;

    public SimulationContext(Island island, GameLoop gameLoop, SpeciesRegistry speciesRegistry, ConsoleView consoleView) {
        this.island = island;
        this.gameLoop = gameLoop;
        this.speciesRegistry = speciesRegistry;
        this.consoleView = consoleView;
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

    public ConsoleView getConsoleView() {
        return consoleView;
    }
}
