package com.island.nature.entities;

import com.island.nature.service.StatisticsService;

/**
 * Interface for reporting lifecycle events and population changes.
 */
public interface NatureStatistics {
    void reportDeath(SpeciesKey key, DeathCause cause);

    void onOrganismAdded(SpeciesKey key);

    void onOrganismRemoved(SpeciesKey key);

    StatisticsService getStatisticsService();
}
