package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.service.CleanupService;
import com.island.service.FeedingService;
import com.island.service.LifecycleService;
import com.island.service.MovementService;
import com.island.service.ReproductionService;
import com.island.util.InteractionMatrix;
import com.island.view.ConsoleView;

/**
 * Responsible for registering simulation tasks in the game loop.
 */
public class TaskRegistry {
    private final GameLoop gameLoop;
    private final Island island;
    private final InteractionMatrix matrix;
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;
    private final ConsoleView view;

    public TaskRegistry(GameLoop gameLoop, Island island, InteractionMatrix matrix, 
                        AnimalFactory animalFactory, SpeciesRegistry speciesRegistry, ConsoleView view) {
        this.gameLoop = gameLoop;
        this.island = island;
        this.matrix = matrix;
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
        this.view = view;
    }

    public void registerAll() {
        gameLoop.addRecurringTask(island::nextTick);
        gameLoop.addRecurringTask(new LifecycleService(island, gameLoop.getTaskExecutor()));
        gameLoop.addRecurringTask(new FeedingService(island, matrix, speciesRegistry, gameLoop.getTaskExecutor()));
        gameLoop.addRecurringTask(new MovementService(island, gameLoop.getTaskExecutor()));
        gameLoop.addRecurringTask(new ReproductionService(island, animalFactory, speciesRegistry, gameLoop.getTaskExecutor()));
        gameLoop.addRecurringTask(new CleanupService(island, gameLoop.getTaskExecutor()));
        gameLoop.addRecurringTask(() -> view.display(island));
    }
}
