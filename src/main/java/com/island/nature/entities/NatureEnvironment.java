package com.island.nature.entities;

import com.island.nature.config.Configuration;
import com.island.nature.service.ProtectionService;
import java.util.Map;

/**
 * Interface for accessing environmental context.
 */
public interface NatureEnvironment {
    Configuration getConfiguration();

    Season getCurrentSeason();

    ProtectionService getProtectionService();

    Map<SpeciesKey, Integer> getProtectionMap();
}
