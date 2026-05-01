package com.island.content;

import com.island.engine.GameLoop;
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
    private final GameLoop<Organism> gameLoop;
    private final NatureWorld world;
    private final InteractionProvider matrix;
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;
    private final SimulationView view;
    private final RandomProvider random;

    public TaskRegistry(GameLoop<Organism> gameLoop, NatureWorld world, InteractionProvider matrix, 
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
        // Food
        gameLoop.addRecurringTask(new FeedingService(world, animalFactory, matrix, speciesRegistry, huntingStrategy, gameLoop.getTaskExecutor(), random));
        // Move
        gameLoop.addRecurringTask(new MovementService(world, speciesRegistry, gameLoop.getTaskExecutor(), random));
        // Repro
        gameLoop.addRecurringTask(new ReproductionService(world, animalFactory, speciesRegistry, gameLoop.getTaskExecutor(), random));
        // Death (Metabolism, Age)
        gameLoop.addRecurringTask(new LifecycleService(world, gameLoop.getTaskExecutor(), random));
        // Cleanup
        gameLoop.addRecurringTask(new CleanupService(world, animalFactory, gameLoop.getTaskExecutor(), random));
        // View
        gameLoop.addRecurringTask(() -> view.display(world.createSnapshot()));
    }
}
