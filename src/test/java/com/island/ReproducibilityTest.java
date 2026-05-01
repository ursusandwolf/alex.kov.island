package com.island;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.island.content.AnimalFactory;
import com.island.content.DefaultHuntingStrategy;
import com.island.content.HuntingStrategy;
import com.island.content.SpeciesKey;
import com.island.content.SpeciesLoader;
import com.island.content.SpeciesRegistry;
import com.island.model.Island;
import com.island.service.FeedingService;
import com.island.service.LifecycleService;
import com.island.service.MovementService;
import com.island.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import com.island.util.RandomProvider;
import com.island.util.RandomUtils;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

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
        // Use a fixed "random" provider
        RandomProvider fixedProvider = new RandomProvider() {
            private int counter = 0;
            @Override public int nextInt(int bound) { return (counter++) % bound; }
            @Override public int nextInt(int origin, int bound) { return origin + (counter++) % (bound - origin); }
            @Override public double nextDouble() { return (double) ((counter++) % 100) / 100.0; }
            @Override public double nextDouble(double bound) { return ((double) ((counter++) % 100) / 100.0) * bound; }
            @Override public long nextLong() { return 0L; }
            @Override public boolean nextBoolean() { return (counter++) % 2 == 0; }
        };
        RandomUtils.setProvider(fixedProvider);

        SpeciesRegistry registry = new SpeciesLoader().load();
        InteractionMatrix matrix = new InteractionMatrix(registry);
        // Simplified matrix for test
        registry.getAllAnimalKeys().forEach(p -> 
            registry.getAllAnimalKeys().forEach(prey -> matrix.setChance(p, prey, 50))
        );
        matrix.freeze();

        Island island = new Island(2, 2, registry, new StatisticsService());
        AnimalFactory factory = new AnimalFactory(registry, fixedProvider);
        HuntingStrategy strategy = new DefaultHuntingStrategy(matrix);
        var executor = Executors.newSingleThreadExecutor();

        // 1. Manually trigger world initialization
        // We'll just add some animals manually to be faster and more controlled
        island.getCell(0, 0).addAnimal(factory.createAnimal(SpeciesKey.WOLF).orElseThrow());
        island.getCell(1, 1).addAnimal(factory.createAnimal(SpeciesKey.RABBIT).orElseThrow());

        // 2. Run one tick of services
        new LifecycleService(island, executor, fixedProvider).tick(1);
        new FeedingService(island, factory, matrix, registry, strategy, executor, fixedProvider).tick(1);
        new MovementService(island, registry, executor, fixedProvider).tick(1);

        String state = island.getSpeciesCounts().toString() + "_" + island.getTotalOrganismCount();
        
        executor.shutdown();
        RandomUtils.setProvider(new DefaultRandomProvider()); // Reset
        return state;
    }
}
