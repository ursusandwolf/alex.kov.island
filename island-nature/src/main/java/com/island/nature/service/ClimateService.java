package com.island.nature.service;

import com.island.engine.scheduling.Phase;
import com.island.engine.scheduling.ScheduledTask;
import com.island.nature.entities.environment.Season;

/**
 * Interface for the climate management system.
 */
public interface ClimateService extends ScheduledTask {
    Season getCurrentSeason();

    int getTemperature();

    @Override
    default Phase phase() {
        return Phase.PREPARE;
    }
}
