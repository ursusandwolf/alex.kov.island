package com.island.nature.entities;

import com.island.engine.GameLoop;
import com.island.nature.service.CleanupService;
import com.island.nature.service.FeedingService;
import com.island.nature.service.LifecycleService;
import com.island.nature.service.MovementService;
import com.island.nature.service.ReproductionService;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import com.island.nature.view.SimulationView;

/**
 * Responsible for registering simulation tasks in the game loop.
 */
public class TaskRegistry {
    public static final int PRIORITY_LIFECYCLE = 90;
    public static final int PRIORITY_FEEDING = 80;
    public static final int PRIORITY_MOVEMENT = 70;
    public static final int PRIORITY_REPRODUCTION = 60;
    public static final int PRIORITY_CLEANUP = 10;
    public static final int PRIORITY_VIEW = 0;

    private final GameLoop<Organism> gameLoop;
    private final NatureWorld world;
    private final InteractionProvider matrix;
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;
    private final SimulationView view;
    private final RandomProvider random;
    private final com.island.engine.event.EventBus eventBus;

    public TaskRegistry(GameLoop<Organism> gameLoop, NatureWorld world, InteractionProvider matrix, 
                        AnimalFactory animalFactory, SpeciesRegistry speciesRegistry, 
                        SimulationView view, RandomProvider random, com.island.engine.event.EventBus eventBus) {
        this.gameLoop = gameLoop;
        this.world = world;
        this.matrix = matrix;
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
        this.view = view;
        this.random = random;
        this.eventBus = eventBus;
    }

    public void registerAll() {
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(world.getConfiguration(), matrix);
        // Food
        gameLoop.addRecurringTask(new FeedingService(world, animalFactory, matrix, speciesRegistry, huntingStrategy, gameLoop.getTaskExecutor(), random, eventBus));
        // Move
        gameLoop.addRecurringTask(new MovementService(world, speciesRegistry, gameLoop.getTaskExecutor(), random));
        // Repro
        gameLoop.addRecurringTask(new ReproductionService(world, animalFactory, speciesRegistry, gameLoop.getTaskExecutor(), random));
        // Death (Metabolism, Age)
        gameLoop.addRecurringTask(new LifecycleService(world, gameLoop.getTaskExecutor(), random, eventBus));
        // Cleanup
        gameLoop.addRecurringTask(new CleanupService(world, animalFactory, gameLoop.getTaskExecutor(), random));
        // View
        gameLoop.addRecurringTask(() -> view.display(world.createSnapshot()));
    }
}
