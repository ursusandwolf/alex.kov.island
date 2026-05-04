package com.island.engine;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.island.nature.config.Configuration;
import com.island.nature.entities.AnimalFactory;
import com.island.nature.entities.NatureDomainContext;
import com.island.nature.entities.SpeciesLoader;
import com.island.nature.entities.SpeciesRegistry;
import com.island.nature.entities.WorldInitializer;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.DefaultRandomProvider;
import com.island.util.InteractionMatrix;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class WorldInitializationTest {

    @Test
    public void testWorldInitializationDensity() {
        Configuration config = new Configuration();
        SpeciesRegistry registry = new SpeciesLoader(config).load();
        StatisticsService statisticsService = new StatisticsService(config);
        DefaultRandomProvider randomProvider = new DefaultRandomProvider();
        AnimalFactory animalFactory = new AnimalFactory(registry, randomProvider);

        NatureDomainContext context = NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .animalFactory(animalFactory)
                .statisticsService(statisticsService)
                .protectionService(new DefaultProtectionService(config, registry, statisticsService, 16 * 16))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .build();

        Island island = new Island(context, 16, 16);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        
        WorldInitializer initializer = new WorldInitializer();
        initializer.initialize(island, registry, animalFactory, executor, new DefaultRandomProvider());
        executor.shutdown();

        int totalOrganisms = island.getTotalOrganismCount();
        System.out.println("Total organisms after initialization: " + totalOrganisms);
        
        assertTrue(totalOrganisms > 1000, "Island should be significantly populated");
        
        var cell = island.getCell(0, 0);
        assertTrue(cell.getAnimalCount() + cell.getPlantCount() > 0, "Cell 0,0 should not be empty");
    }
}
