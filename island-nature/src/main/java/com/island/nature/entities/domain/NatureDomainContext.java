package com.island.nature.entities.domain;

import com.island.engine.core.AgeStorage;
import com.island.engine.core.EntityIdProvider;
import com.island.engine.core.HealthStorage;
import com.island.engine.core.MovementStorage;
import com.island.nature.config.Configuration;
import com.island.nature.service.AlertService;
import com.island.nature.service.ClimateService;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.nature.model.ChunkingStrategy;
import lombok.Builder;
import lombok.Getter;
import com.island.engine.ecs.ComponentRegistry;
import com.island.nature.entities.registry.AnimalFactory;
import com.island.nature.entities.registry.BiomassManager;
import com.island.nature.entities.registry.SpeciesRegistry;
import com.island.util.common.RandomProvider;
import com.island.nature.model.InteractionProvider;

/**
 * Encapsulates domain-specific components for the nature simulation.
 * Provides a single point of injection for registries, factories, and services.
 */
@Getter
@Builder
public class NatureDomainContext {
    private final Configuration config;
    private final EntityIdProvider entityIdProvider;
    private final HealthStorage healthStorage;
    private final AgeStorage ageStorage;
    private final MovementStorage movementStorage;
    private final SpeciesRegistry speciesRegistry;
    private final InteractionProvider interactionProvider;
    private final AnimalFactory animalFactory;
    private final StatisticsService statisticsService;
    private final AlertService alertService;
    private final ClimateService climateService;
    private final ProtectionService protectionService;
    private final BiomassManager biomassManager;
    private final RandomProvider randomProvider;
    private final ComponentRegistry componentRegistry;
    private final ChunkingStrategy chunkingStrategy;
}