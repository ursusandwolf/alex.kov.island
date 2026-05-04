package com.island.simcity;

import com.island.engine.GameLoop;
import com.island.engine.SimulationPlugin;
import com.island.engine.SimulationWorld;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.model.CityMap;
import com.island.simcity.service.BuildingService;
import com.island.simcity.service.CityAnalyticsService;
import com.island.simcity.service.ConnectivityService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;

/**
 * Plugin implementation for the SimCity simulation.
 */
public class SimCityPlugin implements SimulationPlugin<SimEntity> {
    private final int width;
    private final int height;

    public SimCityPlugin(int width, int height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public SimulationWorld<SimEntity, Void> createWorld() {
        return new CityMap(width, height);
    }

    @Override
    public void registerTasks(GameLoop<SimEntity> gameLoop, SimulationWorld<SimEntity, ?> world, com.island.engine.event.EventBus eventBus) {
        CityMap map = (CityMap) world;
        
        ConnectivityService connService = new ConnectivityService(map);
        CityAnalyticsService analyticsService = new CityAnalyticsService(map);
        PopulationService popService = new PopulationService(map);
        EconomyService economyService = new EconomyService(map);

        gameLoop.addRecurringTask(connService);
        gameLoop.addRecurringTask(analyticsService);
        gameLoop.addRecurringTask(popService);
        gameLoop.addRecurringTask(economyService);

        // Cleanup task
        gameLoop.addRecurringTask(t -> {
            for (int x = 0; x < map.getWidth(); x++) {
                for (int y = 0; y < map.getHeight(); y++) {
                    map.getGrid()[x][y].cleanupDeadEntities(e -> { });
                }
            }
        });
    }
}
