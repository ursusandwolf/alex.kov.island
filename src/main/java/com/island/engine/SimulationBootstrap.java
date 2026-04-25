package com.island.engine;

import com.island.config.Configuration;
import com.island.content.AnimalFactory;
import com.island.content.FeedingService;
import com.island.content.SpeciesConfig;
import com.island.model.Island;
import com.island.service.*;
import com.island.util.InteractionMatrix;
import com.island.view.ConsoleView;

/**
 * Responsible for initializing all components of the simulation.
 */
public class SimulationBootstrap {

    public SimulationContext setup() {
        // 1. Load configuration
        Configuration config = Configuration.load();
        SpeciesConfig speciesConfig = SpeciesConfig.getInstance();
        
        // 2. Setup interaction matrix
        InteractionMatrix matrix = new InteractionMatrix();
        initInteractionMatrix(matrix, speciesConfig);

        // 3. Create core models
        Island island = new Island(config.getIslandWidth(), config.getIslandHeight());
        AnimalFactory animalFactory = new AnimalFactory(speciesConfig);

        // 4. Setup GameLoop and View
        GameLoop gameLoop = new GameLoop(config.getTickDurationMs());
        ConsoleView consoleView = new ConsoleView();

        // 5. Initialize world population
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, speciesConfig, animalFactory, gameLoop.getTaskExecutor());

        // 6. Register simulation tasks
        registerTasks(gameLoop, island, matrix, animalFactory, consoleView);

        return new SimulationContext(island, gameLoop, speciesConfig, consoleView);
    }

    private void initInteractionMatrix(InteractionMatrix matrix, SpeciesConfig config) {
        for (String predatorKey : config.getAllSpeciesKeys()) {
            for (String preyKey : config.getAllSpeciesKeys()) {
                int chance = config.getHuntProbability(predatorKey, preyKey);
                if (chance > 0) {
                    matrix.setChance(predatorKey, preyKey, chance);
                }
            }
            // Default plant eating chance if configured in properties or hardcoded fallback
            int plantChance = config.getHuntProbability(predatorKey, "plant");
            if (plantChance > 0) {
                matrix.setChance(predatorKey, "Plant", plantChance);
            } else if (predatorKey.equals("rabbit") || predatorKey.equals("duck") || predatorKey.equals("goat")) {
                matrix.setChance(predatorKey, "Plant", 100);
            }
        }
    }

    private void registerTasks(GameLoop loop, Island island, InteractionMatrix matrix, 
                               AnimalFactory factory, ConsoleView view) {
        loop.addRecurringTask(island::nextTick);
        loop.addRecurringTask(new LifecycleService(island, loop.getTaskExecutor()));
        loop.addRecurringTask(new FeedingService(island, matrix, loop.getTaskExecutor()));
        loop.addRecurringTask(new MovementService(island, loop.getTaskExecutor()));
        loop.addRecurringTask(new ReproductionService(island, factory, loop.getTaskExecutor()));
        loop.addRecurringTask(new CleanupService(island, loop.getTaskExecutor()));
        loop.addRecurringTask(() -> view.display(island));
    }
}
