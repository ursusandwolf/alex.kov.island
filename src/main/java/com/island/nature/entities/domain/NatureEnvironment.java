package com.island.nature.entities.domain;

import com.island.nature.config.Configuration;
import com.island.nature.service.ProtectionService;
import java.util.Map;
import com.island.nature.entities.core.SpeciesKey;
import com.island.nature.entities.environment.Season;

/**
 * Interface for accessing environmental context.
 */
public interface NatureEnvironment {
    Configuration getConfiguration();

    Season getCurrentSeason();

    ProtectionService getProtectionService();

    Map<SpeciesKey, Integer> getProtectionMap();
}