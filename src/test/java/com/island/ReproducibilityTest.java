package com.island;

import com.island.engine.event.DefaultEventBus;
import com.island.nature.config.Configuration;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.FeedingService;
import com.island.nature.service.HealthSystem;
import com.island.nature.service.MovementSystem;
import com.island.nature.service.StatisticsService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.entities.strategy.DefaultHuntingStrategy;
import com.island.nature.entities.strategy.HuntingStrategy;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;
import com.island.util.common.RandomUtils;
import com.island.util.interaction.InteractionMatrix;

class ReproducibilityTest {

    @Test
    void testSimulationReproducibility() {
        // Run 1
        String state1 = runSimulationAndGetState();
        
        // Run 2
        String state2 = runSimulationAndGetState();

        assertEquals(state1, state2, "Simulation results should be identical with the same random sequence");
    }

    private String runSimulationAndGetState() {
        Configuration config = new Configuration();
        // Use a fixed "random" provider
        RandomProvider fixedProvider = new RandomProvider() {
            @Override public int nextInt(int bound) { return (counter++) % bound; }
            @Override public int nextInt(int origin, int bound) { return origin + (counter++) % (bound - origin); }
            @Override public double nextDouble() { return (double) ((counter++) % 100) / 100.0; }
            @Override public double nextDouble(double bound) { return ((double) ((counter++) % 100) / 100.0) * bound; }
            @Override public long nextLong() { return 0L; }
            @Override public boolean nextBoolean() { return (counter++) % 2 == 0; }
            private int counter = 0;
        };
        RandomUtils.setProvider(fixedProvider);

        SpeciesRegistry registry = new SpeciesLoader(config).load();
        InteractionMatrix matrix = new InteractionMatrix(registry);
        // Simplified matrix for test
        registry.getAllAnimalKeys().forEach(p -> 
            registry.getAllAnimalKeys().forEach(prey -> matrix.setChance(p, prey, 50))
        );
        matrix.freeze();

        StatisticsService statisticsService = new StatisticsService(config);
        AnimalFactory factory = new AnimalFactory(registry, fixedProvider);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(matrix)
                .animalFactory(factory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 4))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(fixedProvider)
                .build();

        Island island = new Island(context, 2, 2, new DefaultEventBus());
        HuntingStrategy strategy = new DefaultHuntingStrategy(config, matrix);
        var executor = Executors.newSingleThreadExecutor();

        // 1. Manually trigger world initialization
        // We'll just add some animals manually to be faster and more controlled
        island.getCell(0, 0).addAnimal(factory.createAnimal(new SpeciesKey("wolf", true)).orElseThrow());
        island.getCell(1, 1).addAnimal(factory.createAnimal(new SpeciesKey("rabbit", false)).orElseThrow());

        // 2. Run one tick of services
        new HealthSystem(island, executor, fixedProvider).tick(1);
        new FeedingService(island, factory, matrix, registry, strategy, executor, fixedProvider).tick(1);
        new MovementSystem(island, executor, fixedProvider).tick(1);

        String state = island.getSpeciesCounts().toString() + "_" + island.getTotalOrganismCount();
        
        executor.shutdown();
        RandomUtils.setProvider(new DefaultRandomProvider()); // Reset
        return state;
    }
}