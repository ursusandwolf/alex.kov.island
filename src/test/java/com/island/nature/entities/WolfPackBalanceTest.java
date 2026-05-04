package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.nature.entities.predators.Bear;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.FeedingService;
import com.island.engine.event.DefaultEventBus;
import com.island.nature.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class WolfPackBalanceTest {

    @Test
    void compareSoloVsPackPerformanceAndBalance() {
        Configuration config = new Configuration();
        SpeciesRegistry registry = new SpeciesLoader(config).load();
        
        System.out.println("\n=== WOLF PACK BALANCE & PERFORMANCE REPORT ===");
        
        // Warmup to stabilize JIT
        runSimulation(registry, true, 1, config);
        runSimulation(registry, false, 1, config);

        long packTime = runSimulation(registry, true, 100, config); 
        long soloTime = runSimulation(registry, false, 100, config);

        System.out.println("Pack Hunting (avg): " + (packTime / 100) + " ns");
        System.out.println("Solo Hunting (avg): " + (soloTime / 100) + " ns");
        
        double speedup = (double) soloTime / packTime;
        System.out.println("Speedup factor: " + String.format("%.2f", speedup) + "x");
    }

    private long runSimulation(SpeciesRegistry registry, boolean usePack, int iterations, Configuration config) {
        config.setWolfPackMinSize(usePack ? 3 : 1000);
        StatisticsService statisticsService = new StatisticsService(config);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        AnimalFactory animalFactory = new AnimalFactory(registry, randomProvider);
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(matrix)
                .animalFactory(animalFactory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 1))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .build();

        Island island = new Island(context, 1, 1);
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        
        List<GenericAnimal> wolves = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
            wolf.setEnergy((wolf.getMaxEnergy() * 30) / 100);
            cell.addAnimal(wolf);
            wolves.add(wolf);
        }

        for (int i = 0; i < 5; i++) {
            Bear bear = new Bear(registry.getAnimalType(SpeciesKey.BEAR).orElseThrow());
            for(int a=0; a<60; a++) { bear.checkAgeDeath(); }
            cell.addAnimal(bear);
        }

        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(config, matrix);
        FeedingService service = new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, 
                                            Executors.newSingleThreadExecutor(), new DefaultRandomProvider(), new DefaultEventBus());

        long totalTime = 0;
        int lastSurvivors = 0;
        double lastAvgEnergy = 0;

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            service.tick(1);
            totalTime += (System.nanoTime() - start);
            
            int survivors = 0;
            long totalEnergy = 0;
            for (GenericAnimal wolf : wolves) {
                if (wolf.isAlive()) {
                    survivors++;
                    totalEnergy += wolf.getCurrentEnergy();
                }
            }
            lastSurvivors = survivors;
            lastAvgEnergy = survivors > 0 ? ((double) totalEnergy / survivors / config.getScale1M()) : 0;
        }

        if (iterations == 100) {
            System.out.println((usePack ? "[PACK]" : "[SOLO]") + " Survivors: " + lastSurvivors + ", Avg Energy: " + String.format("%.2f", lastAvgEnergy));
        }

        return totalTime;
    }
}
