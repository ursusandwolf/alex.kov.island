package com.island.simcity;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.EventBus;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.EconomyComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import com.island.simcity.service.BuildingService;
import com.island.simcity.service.CityAnalyticsService;
import com.island.simcity.service.ConnectivityService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import com.island.simcity.service.PollutionService;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.scheduling.GameLoop;
import java.util.List;

/**
 * Plugin implementation for the SimCity simulation.
 */
public class SimCityPlugin implements SimulationPlugin<SimEntity> {
    private final int width;
    private final int height;
    private final ComponentRegistry componentRegistry = new ComponentRegistry();

    public SimCityPlugin() {
        this(10, 10);
    }

    public SimCityPlugin(int width, int height) {
        this.width = width;
        this.height = height;
        // Register components for stable indices
        componentRegistry.getBitSet(List.of(
                PopulationComponent.class,
                BuildingComponent.class,
                EconomyComponent.class
        ));
    }

    @Override
    public SimulationWorld<SimEntity> createWorld(EventBus eventBus) {
        return new CityMap(width, height, eventBus, componentRegistry);
    }

    @Override
    public void registerTasks(GameLoop<SimEntity> gameLoop, SimulationWorld<SimEntity> world, EventBus eventBus) {
        CityMap map = (CityMap) world;

        ConnectivityService connService = new ConnectivityService(map);
        CityAnalyticsService analyticsService = new CityAnalyticsService(map);
        PopulationService popService = new PopulationService(map, componentRegistry);
        EconomyService economyService = new EconomyService(map);
        PollutionService pollutionService = new PollutionService(map);

        gameLoop.addRecurringTask(connService);
        gameLoop.addRecurringTask(analyticsService);
        gameLoop.addRecurringTask(popService);
        gameLoop.addRecurringTask(economyService);
        gameLoop.addRecurringTask(pollutionService);

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