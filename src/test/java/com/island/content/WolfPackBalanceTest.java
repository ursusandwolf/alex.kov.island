package com.island.content;

import com.island.model.Cell;
import com.island.model.Island;
import com.island.util.InteractionMatrix;
import com.island.util.DefaultRandomProvider;
import com.island.service.FeedingService;
import com.island.service.StatisticsService;
import com.island.content.animals.predators.Bear;
import org.junit.jupiter.api.Test;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WolfPackBalanceTest {

    @Test
    void compareSoloVsPackPerformanceAndBalance() {
        SpeciesRegistry registry = new SpeciesLoader().load();
        
        System.out.println("\n=== WOLF PACK BALANCE & PERFORMANCE REPORT ===");
        
        // Warmup to stabilize JIT
        runSimulation(registry, true, 1);
        runSimulation(registry, false, 1);

        long packTime = runSimulation(registry, true, 100); // 100 iterations for stable average
        long soloTime = runSimulation(registry, false, 100);

        System.out.println("Pack Hunting (avg): " + (packTime / 100) + " ns");
        System.out.println("Solo Hunting (avg): " + (soloTime / 100) + " ns");
        
        double speedup = (double) soloTime / packTime;
        System.out.println("Speedup factor: " + String.format("%.2f", speedup) + "x");
        
        // Note: Pack is doing MORE work (actually eating bears), so speedup < 1 is normal
        // but it shows the complexity is manageable.
    }

    private long runSimulation(SpeciesRegistry registry, boolean usePack, int iterations) {
        Island island = new Island(1, 1, registry, new StatisticsService());
        island.setRedBookProtectionEnabled(false);
        Cell cell = island.getCell(0, 0);
        InteractionMatrix matrix = InteractionMatrix.buildFrom(registry);
        
        List<GenericAnimal> wolves = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            GenericAnimal wolf = new GenericAnimal(registry.getAnimalType(SpeciesKey.WOLF).orElseThrow());
            wolf.setEnergy(wolf.getMaxEnergy() * 0.3);
            cell.addAnimal(wolf);
            wolves.add(wolf);
        }

        for (int i = 0; i < 5; i++) {
            Bear bear = new Bear(registry.getAnimalType(SpeciesKey.BEAR).orElseThrow());
            for(int a=0; a<60; a++) bear.checkAgeDeath(); 
            cell.addAnimal(bear);
        }

        HuntingStrategy huntingStrategy = new DefaultHuntingStrategy(matrix);
        int testMinPackSize = usePack ? 3 : 1000;
        AnimalFactory animalFactory = new AnimalFactory(registry, new DefaultRandomProvider());
        FeedingService service = new FeedingService(island, animalFactory, matrix, registry, huntingStrategy, 
                                            Executors.newSingleThreadExecutor(), testMinPackSize, new DefaultRandomProvider());

        long totalTime = 0;
        int lastSurvivors = 0;
        double lastAvgEnergy = 0;

        for (int i = 0; i < iterations; i++) {
            // Reset state for each iteration if multiple (not needed for this specific logic check)
            long start = System.nanoTime();
            service.tick(1);
            totalTime += (System.nanoTime() - start);
            
            int survivors = 0;
            double totalEnergy = 0;
            for (GenericAnimal wolf : wolves) {
                if (wolf.isAlive()) {
                    survivors++;
                    totalEnergy += wolf.getCurrentEnergy();
                }
            }
            lastSurvivors = survivors;
            lastAvgEnergy = survivors > 0 ? (totalEnergy / survivors) : 0;
        }

        if (iterations == 100) {
            System.out.println((usePack ? "[PACK]" : "[SOLO]") + " Survivors: " + lastSurvivors + ", Avg Energy: " + String.format("%.2f", lastAvgEnergy));
        }

        return totalTime;
    }
}
