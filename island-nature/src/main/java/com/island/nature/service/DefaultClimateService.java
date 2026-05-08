package com.island.nature.service;

import com.island.nature.config.Configuration;
import com.island.nature.entities.environment.Season;
import com.island.util.common.RandomProvider;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Manages global climate and seasons.
 */
@Getter
@RequiredArgsConstructor
public class DefaultClimateService implements ClimateService {
    private final Configuration config;
    private final RandomProvider random;

    private Season currentSeason = Season.SPRING;
    private int temperature = 20;

    @Override
    public int priority() {
        return 100; // Run before everything else in PREPARE phase
    }

    @Override
    public void tick(int tickCount) {
        updateSeason(tickCount);
        updateTemperature(tickCount);
    }

    private void updateSeason(int tickCount) {
        int seasonDuration = config.getSeasonDuration();
        int seasonIndex = (tickCount / seasonDuration) % 4;
        this.currentSeason = Season.values()[seasonIndex];
    }

    private void updateTemperature(int tickCount) {
        int base = config.getBaseTemperature();
        int delta = 0;
        switch (currentSeason) {
            case WINTER -> delta = config.getWinterTemperatureDelta();
            case SUMMER -> delta = config.getSummerTemperatureDelta();
            case SPRING, AUTUMN -> delta = 0;
            default -> delta = 0;
        }
        
        int fluctuation = random.nextInt(-config.getTemperatureFluctuationRange(), 
                                         config.getTemperatureFluctuationRange() + 1);
        this.temperature = base + delta + fluctuation;
    }
}
