package com.island.engine;

import com.island.content.AnimalFactory;
import com.island.content.DefaultHuntingStrategy;
import com.island.content.HuntingStrategy;
import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.service.CleanupService;
import com.island.service.FeedingService;
import com.island.service.LifecycleService;
import com.island.service.MovementService;
import com.island.service.ReproductionService;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import com.island.view.SimulationView;

/**
 * Responsible for registering simulation tasks in the game loop.
 */
public class TaskRegistry {
    private final GameLoop gameLoop;
    private final SimulationWorld world;
    private final InteractionProvider matrix;
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;
    private final SimulationView view;
    private final RandomProvider random;

    public TaskRegistry(GameLoop gameLoop, SimulationWorld world, InteractionProvider matrix, 
                        AnimalFactory animalFactory, SpeciesRegistry speciesRegistry, 
                        SimulationView view, RandomProvider random) {
        this.gameLoop = gameLoop;
        this.world = world;
        this.matrix = matrix;
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
        this.view = view;
        this.random = random;
    }

    public void registerAll() {
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        gameLoop.addRecurringTask(world);
        gameLoop.addRecurringTask(new LifecycleService(world, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new FeedingService(world, animalFactory, matrix, speciesRegistry, huntingStrategy, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new MovementService(world, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new ReproductionService(world, animalFactory, speciesRegistry, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new CleanupService(world, animalFactory, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(() -> {
            if (world instanceof com.island.model.Island island) {
                view.display(island);
            }
        });
    }
}
