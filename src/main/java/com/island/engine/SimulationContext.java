package com.island.engine;

import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.view.ConsoleView;
import lombok.Getter;

@Getter
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
}
