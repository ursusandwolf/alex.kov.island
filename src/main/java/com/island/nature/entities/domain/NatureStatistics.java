package com.island.nature.entities.domain;

import com.island.nature.service.StatisticsService;

/**
 * Interface for reporting lifecycle events and population changes.
 */
public interface NatureStatistics {
    /**
     * Returns the underlying statistics service.
     * @return The statistics service.
     */
    StatisticsService getStatisticsService();
}