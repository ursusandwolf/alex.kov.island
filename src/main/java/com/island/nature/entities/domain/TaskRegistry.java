package com.island.nature.entities.domain;

import com.island.engine.event.EventBus;
import com.island.nature.service.AnimalFeedingSystem;
import com.island.nature.service.AnimalHealthSystem;
import com.island.nature.service.AnimalMovementSystem;
import com.island.nature.service.AnimalReproductionSystem;
import com.island.nature.service.BiomassGrowthSystem;
import com.island.nature.service.BiomassMovementSystem;
import com.island.nature.service.CleanupService;
import com.island.nature.view.SimulationView;
import com.island.engine.scheduling.GameLoop;
import com.island.nature.entities.core.Organism;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.nature.entities.strategy.HuntingStrategy;
import com.island.util.common.RandomProvider;
import com.island.util.interaction.InteractionProvider;

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
    private final NatureDomainContext domainContext;
    private final InteractionProvider matrix;
    private final AnimalFactory animalFactory;
    private final SpeciesRegistry speciesRegistry;
    private final SimulationView view;
    private final RandomProvider random;
    private final EventBus eventBus;

    public TaskRegistry(GameLoop<Organism> gameLoop, NatureWorld world, NatureDomainContext domainContext, InteractionProvider matrix, 
                        AnimalFactory animalFactory, SpeciesRegistry speciesRegistry, 
                        SimulationView view, RandomProvider random, EventBus eventBus) {
        this.gameLoop = gameLoop;
        this.world = world;
        this.domainContext = domainContext;
        this.matrix = matrix;
        this.animalFactory = animalFactory;
        this.speciesRegistry = speciesRegistry;
        this.view = view;
        this.random = random;
        this.eventBus = eventBus;
    }

    public void registerAll() {
        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(world.getConfiguration(), matrix);
        // Climate
        gameLoop.addRecurringTask(domainContext.getClimateService());
        // Food
        gameLoop.addRecurringTask(new AnimalFeedingSystem(world, animalFactory, matrix, speciesRegistry, huntingStrategy, gameLoop.getTaskExecutor(), random));
        // Move
        gameLoop.addRecurringTask(new AnimalMovementSystem(world, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new BiomassMovementSystem(world, gameLoop.getTaskExecutor(), random));
        // Repro
        gameLoop.addRecurringTask(new AnimalReproductionSystem(world, animalFactory, speciesRegistry, gameLoop.getTaskExecutor(), random));
        // Death (Metabolism, Age)
        gameLoop.addRecurringTask(new AnimalHealthSystem(world, gameLoop.getTaskExecutor(), random));
        gameLoop.addRecurringTask(new BiomassGrowthSystem(world, gameLoop.getTaskExecutor(), random));
        // Cleanup
        gameLoop.addRecurringTask(new CleanupService(world, animalFactory, gameLoop.getTaskExecutor(), random));
        // View
        gameLoop.addRecurringTask(() -> view.display(world.createSnapshot()));
    }
}
