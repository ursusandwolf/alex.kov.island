package com.island.engine;

import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.view.ConsoleView;

/**
 * Context that holds all major components of a running simulation.
 */
public class SimulationContext {
    private final Island island;
    private final GameLoop gameLoop;
    private final SpeciesConfig speciesConfig;
    private final ConsoleView consoleView;

    public SimulationContext(Island island, GameLoop gameLoop, SpeciesConfig speciesConfig, ConsoleView consoleView) {
        this.island = island;
        this.gameLoop = gameLoop;
        this.speciesConfig = speciesConfig;
        this.consoleView = consoleView;
    }

    public Island getIsland() {
        return island;
    }

    public GameLoop getGameLoop() {
        return gameLoop;
    }

    public SpeciesConfig getSpeciesConfig() {
        return speciesConfig;
    }

    public ConsoleView getConsoleView() {
        return consoleView;
    }
}
