package com.island.nature.entities.domain;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.core.AgeStorage;
import com.island.engine.core.EntityIdProvider;
import com.island.engine.core.HealthStorage;
import com.island.nature.config.Configuration;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.model.ChunkingStrategy;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.DynamicChunkingStrategy;
import com.island.nature.model.StaticChunkingStrategy;
import com.island.nature.service.AlertService;
import com.island.nature.service.DefaultClimateService;
import com.island.nature.service.DefaultProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.common.DefaultRandomProvider;
import com.island.util.common.RandomProvider;
import com.island.nature.model.InteractionMatrix;

/**
 * Factory for creating NatureDomainContext instances.
 * Encapsulates the assembly of nature-domain services and registries.
 */
public class NatureDomainContextFactory {

    public static NatureDomainContext create(Configuration config) {
        SpeciesRegistry speciesRegistry = new SpeciesLoader(config).load();
        StatisticsService statisticsService = new StatisticsService(config);
        RandomProvider randomProvider = new DefaultRandomProvider();
        ComponentRegistry componentRegistry = new ComponentRegistry();
        
        ChunkingStrategy chunkingStrategy = config.isDynamicChunkingEnabled() 
                ? new DynamicChunkingStrategy(config)
                : new StaticChunkingStrategy(config);
        
        int capacity = config.getIslandWidth() * config.getIslandHeight() * 10;
        
        return NatureDomainContext.builder()
                .config(config)
                .entityIdProvider(EntityIdProvider.create())
                .healthStorage(HealthStorage.create(capacity))
                .ageStorage(AgeStorage.create(capacity))
                .speciesRegistry(speciesRegistry)
                .interactionProvider(InteractionMatrix.buildFrom(speciesRegistry))
                .statisticsService(statisticsService)
                .alertService(new AlertService())
                .climateService(new DefaultClimateService(config, randomProvider))
                .animalFactory(new AnimalFactory(speciesRegistry, randomProvider, componentRegistry))
                .protectionService(new DefaultProtectionService(config, speciesRegistry, statisticsService, 
                                     config.getIslandWidth() * config.getIslandHeight()))
                .biomassManager(new DefaultBiomassManager())
                .randomProvider(randomProvider)
                .componentRegistry(componentRegistry)
                .chunkingStrategy(chunkingStrategy)
                .build();
    }
}
