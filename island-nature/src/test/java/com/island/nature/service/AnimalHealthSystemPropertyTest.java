package com.island.nature.service;

import com.island.engine.event.EventBus;
import com.island.nature.config.Configuration;
import com.island.nature.entities.core.Animal;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.domain.NatureDomainContext;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.SpeciesLoader;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.nature.model.Cell;
import com.island.nature.model.DefaultBiomassManager;
import com.island.nature.model.Island;
import com.island.nature.model.StaticChunkingStrategy;
import com.island.nature.model.InteractionMatrix;
import com.island.util.common.DefaultRandomProvider;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.entities.environment.Season;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.Executors;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AnimalHealthSystemPropertyTest {

    private final Configuration config = new Configuration();
    private final DefaultRandomProvider random = new DefaultRandomProvider();
    private final SpeciesRegistry registry = new SpeciesLoader(config).load();

    @Property
    void energyNeverIncreasesAndIsNeverNegative(@ForAll @IntRange(min = 1, max = 1000) int initialEnergy,
                                                @ForAll Season season) {
        NatureDomainContext domainContext = createDomainContext();
        ReflectionTestUtils.setField(domainContext.getClimateService(), "currentSeason", season);
        
        Island island = new Island(domainContext, 1, 1, EventBus.create());
        Cell cell = island.getCell(0, 0);
        AnimalHealthSystem healthSystem = new AnimalHealthSystem(island, Executors.newSingleThreadExecutor(), random);

        SpeciesKey wolfKey = new SpeciesKey("wolf", true);
        Animal wolf = domainContext.getAnimalFactory().createAnimal(wolfKey).orElseThrow();
        domainContext.getHealthStorage().setCurrentEnergy(wolf.getEntityId(), initialEnergy);
        cell.addAnimal(wolf);

        healthSystem.processCell(cell, 1);

        long resultEnergy = wolf.getCurrentEnergy();
        assertThat(resultEnergy).isLessThanOrEqualTo(initialEnergy);
        assertThat(resultEnergy).isGreaterThanOrEqualTo(0);
        
        healthSystem.getExecutor().shutdown();
    }

    private NatureDomainContext createDomainContext() {
        ComponentRegistry componentRegistry = new ComponentRegistry();
        StatisticsService stats = new StatisticsService(config);
        com.island.engine.core.EntityIdProvider idProvider = com.island.engine.core.EntityIdProvider.create();
        com.island.engine.core.HealthStorage healthStorage = com.island.engine.core.HealthStorage.create(100);
        com.island.engine.core.AgeStorage ageStorage = com.island.engine.core.AgeStorage.create(100);
        com.island.engine.core.MovementStorage movementStorage = com.island.engine.core.MovementStorage.create(100);

        AnimalFactory animalFactory = new AnimalFactory(registry, random, componentRegistry, idProvider, healthStorage, ageStorage, movementStorage);

        return NatureDomainContext.builder()
                .config(config)
                .speciesRegistry(registry)
                .interactionProvider(InteractionMatrix.buildFrom(registry))
                .statisticsService(stats)
                .alertService(new AlertService())
                .climateService(new DefaultClimateService(config, random))
                .animalFactory(animalFactory)
                .protectionService(new DefaultProtectionService(config, registry, stats, 1))
                .biomassManager(new DefaultBiomassManager())
                .chunkingStrategy(new StaticChunkingStrategy(config))
                .randomProvider(random)
                .componentRegistry(componentRegistry)
                .entityIdProvider(idProvider)
                .healthStorage(healthStorage)
                .ageStorage(ageStorage)
                .movementStorage(movementStorage)
                .build();
    }
}
