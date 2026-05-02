package com.island.nature.entities;

import com.island.nature.service.StatisticsService;

/**
 * Interface for reporting lifecycle events and population changes.
 */
public interface NatureStatistics {
    /**
     * Reports the death of an organism with a specific cause.
     * @param key The species key of the deceased organism.
     * @param cause The cause of death.
     */
    void reportDeath(SpeciesKey key, DeathCause cause);

    /**
     * Reports the birth or manual addition of an organism.
     * @param key The species key of the added organism.
     */
    void onOrganismAdded(SpeciesKey key);

    /**
     * Reports the removal of an organism from the world (e.g., eaten or moved).
     * @param key The species key of the removed organism.
     */
    void onOrganismRemoved(SpeciesKey key);

    /**
     * Returns the underlying statistics service.
     * @return The statistics service.
     */
    StatisticsService getStatisticsService();
}
