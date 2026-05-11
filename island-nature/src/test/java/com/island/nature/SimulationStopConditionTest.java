package com.island.nature;

import com.island.engine.core.SimulationConfig;
import com.island.engine.core.SimulationContext;
import com.island.engine.core.SimulationEngine;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.DeathCause;
import com.island.nature.entities.core.Organism;
import com.island.nature.model.Cell;
import com.island.nature.model.Island;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;

class SimulationStopConditionTest {

    @Test
    void testNatureShouldStopWhenExtinct() throws InterruptedException {
        Configuration config = new Configuration();
        NaturePlugin plugin = new NaturePlugin(config);
        SimulationEngine<Organism> engine = new SimulationEngine<>();

        SimulationConfig simConfig = SimulationConfig.defaultFor(2);
        SimulationContext<Organism> context = engine.build(plugin, simConfig);
        Island island = (Island) context.world();

        // Manually kill all animals
        for (int x = 0; x < island.getWidth(); x++) {
            for (int y = 0; y < island.getHeight(); y++) {
                Cell cell = island.getCell(x, y);
                new ArrayList<>(cell.getEntities()).forEach(e -> {
                    if (e instanceof Animal a) {
                        a.die(DeathCause.HUNGER);
                        cell.removeEntity(a);
                    }
                });
            }
        }

        GameLoop<Organism> gameLoop = context.gameLoop();
        gameLoop.start();

        // Wait for loop to run at least one tick and stop
        Thread.sleep(500);

        assertFalse(gameLoop.isRunning(), "GameLoop should have stopped due to extinction.");
        context.close();
    }
}
