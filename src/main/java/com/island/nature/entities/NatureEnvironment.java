package com.island.nature.entities;

import com.island.nature.service.ProtectionService;
import java.util.Map;

/**
 * Interface for accessing environmental context.
 */
public interface NatureEnvironment {
    Season getCurrentSeason();

    ProtectionService getProtectionService();

    Map<SpeciesKey, Integer> getProtectionMap();
}
