package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.scheduling.Phase;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;

/**
 * Service that calculates the desirability of each tile based on infrastructure,
 * pollution, and surrounding environment.
 */
public class DesirabilityService extends AbstractSimCityService {
    private final CityMap map;

    public DesirabilityService(CityMap map) {
        this.map = map;
    }

    @Override
    public Phase phase() {
        return Phase.PREPARE;
    }

    @Override
    public int priority() {
        return 10; // Last in PREPARE
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        int score = 50; // Base desirability

        // Infrastructure bonuses
        if (tile.isConnected()) score += 15;
        if (tile.isWatered()) score += 10;
        if (tile.isPowered()) score += 10;
        if (tile.isRailed()) score += 10;
        if (tile.isMetroConnected()) score += 15;

        // Social services bonuses
        score += tile.getEducationLevel() / 2;
        score += tile.getHealthLevel() / 2;

        // Pollution penalties
        score -= tile.getAirPollution() / 5;
        score -= tile.getWaterPollution() / 5;

        tile.setDesirability(Math.max(0, Math.min(100, score)));
    }
}
