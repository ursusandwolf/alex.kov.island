package com.island.simcity;

import com.island.engine.ecs.ComponentRegistry;
import com.island.engine.event.EventBus;
import com.island.engine.model.NodeSnapshot;
import com.island.engine.model.WorldSnapshot;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.entities.components.EconomyComponent;
import com.island.simcity.entities.components.PopulationComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import com.island.simcity.service.CityAnalyticsService;
import com.island.simcity.service.ConnectivityService;
import com.island.simcity.service.EconomyService;
import com.island.simcity.service.PopulationService;
import com.island.simcity.service.PollutionService;
import com.island.simcity.service.DesirabilityService;
import com.island.simcity.service.SocialService;
import com.island.simcity.service.ZoningService;
import com.island.engine.core.NamedSimulationPlugin;
import com.island.engine.core.SimulationPlugin;
import com.island.engine.core.SimulationWorld;
import com.island.engine.scheduling.GameLoop;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Plugin implementation for the SimCity simulation.
 */
@Component
public class SimCityPlugin implements NamedSimulationPlugin<SimEntity> {
    private final int width;
    private final int height;
    private final ComponentRegistry componentRegistry = new ComponentRegistry();
    private final WorldSnapshot initialSnapshot;
    private final List<com.island.simcity.service.SocialEffectProvider> socialEffectProviders;

    public SimCityPlugin() {
        this(20, 20, null, new java.util.ArrayList<>());
    }

    public SimCityPlugin(int width, int height) {
        this(width, height, null, new java.util.ArrayList<>());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public SimCityPlugin(List<com.island.simcity.service.SocialEffectProvider> socialEffectProviders) {
        this(20, 20, null, socialEffectProviders);
    }

    public SimCityPlugin(int width, int height, WorldSnapshot initialSnapshot, List<com.island.simcity.service.SocialEffectProvider> socialEffectProviders) {
        this.width = initialSnapshot != null ? initialSnapshot.getWidth() : width;
        this.height = initialSnapshot != null ? initialSnapshot.getHeight() : height;
        this.initialSnapshot = initialSnapshot;
        this.socialEffectProviders = socialEffectProviders != null ? socialEffectProviders : new java.util.ArrayList<>();
        // Register components for stable indices
        componentRegistry.getBitSet(List.of(
                PopulationComponent.class,
                BuildingComponent.class,
                EconomyComponent.class
        ));
    }

    @Override
    public String getPluginName() {
        return "simcity";
    }

    @Override
    public SimulationPlugin<SimEntity> withConfiguration(int width, int height, WorldSnapshot snapshot) {
        return new SimCityPlugin(width, height, snapshot, socialEffectProviders);
    }

    @Override
    public SimulationWorld<SimEntity> createWorld(EventBus eventBus) {
        CityMap map = new CityMap(width, height, eventBus, componentRegistry);
        
        if (initialSnapshot != null) {
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    NodeSnapshot nodeSnapshot = initialSnapshot.getNodeSnapshot(x, y);
                    if (nodeSnapshot != null && nodeSnapshot.getEntityCounts() != null) {
                        CityTile tile = map.getGrid()[x][y];
                        for (Map.Entry<String, Integer> entry : nodeSnapshot.getEntityCounts().entrySet()) {
                            try {
                                BuildingComponent.Type type = BuildingComponent.Type.valueOf(entry.getKey().toUpperCase());
                                int count = entry.getValue();
                                for (int i = 0; i < count; i++) {
                                    SimEntity building = new SimEntity(componentRegistry);
                                    building.addComponent(BuildingComponent.builder().type(type).build());
                                    tile.addEntity(building);
                                }
                            } catch (IllegalArgumentException e) {
                                // Ignore unknown types
                            }
                        }
                    }
                }
            }
        }
        
        return map;
    }

    @Override
    public void registerTasks(GameLoop<SimEntity> gameLoop, SimulationWorld<SimEntity> world, EventBus eventBus) {
        CityMap map = (CityMap) world;

        ConnectivityService connService = new ConnectivityService(map);
        CityAnalyticsService analyticsService = new CityAnalyticsService(map);
        SocialService socialService = new SocialService(map, socialEffectProviders);
        PopulationService popService = new PopulationService(map, componentRegistry);
        EconomyService economyService = new EconomyService(map);
        PollutionService pollutionService = new PollutionService(map);
        DesirabilityService desirabilityService = new DesirabilityService(map);
        ZoningService zoningService = new ZoningService(map);

        gameLoop.addRecurringTask(connService);
        gameLoop.addRecurringTask(analyticsService);
        gameLoop.addRecurringTask(socialService);
        gameLoop.addRecurringTask(popService);
        gameLoop.addRecurringTask(economyService);
        gameLoop.addRecurringTask(pollutionService);
        gameLoop.addRecurringTask(desirabilityService);
        gameLoop.addRecurringTask(zoningService);

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
