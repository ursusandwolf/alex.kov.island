package com.island.nature.entities;

import lombok.Getter;

/**
 * Manages simulation seasons and their impact.
 */
public class SeasonManager {
    private static final Season[] SEASONS = Season.values();
    private static final int SEASON_DURATION = 50;

    @Getter
    private Season currentSeason = Season.SPRING;

    public void update(int tickCount) {
        int seasonIndex = (tickCount / SEASON_DURATION) % 4;
        this.currentSeason = SEASONS[seasonIndex];
    }
}