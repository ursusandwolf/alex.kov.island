package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.nature.service.ProtectionService;
import com.island.nature.service.StatisticsService;
import com.island.util.InteractionProvider;
import com.island.util.RandomProvider;
import lombok.Builder;
import lombok.Getter;

/**
 * Encapsulates domain-specific components for the nature simulation.
 * Provides a single point of injection for registries, factories, and services.
 */
@Getter
@Builder
public class NatureDomainContext {
    private final Configuration config;
    private final SpeciesRegistry speciesRegistry;
    private final InteractionProvider interactionProvider;
    private final AnimalFactory animalFactory;
    private final StatisticsService statisticsService;
    private final ProtectionService protectionService;
    private final BiomassManager biomassManager;
    private final RandomProvider randomProvider;
}
