package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.core.SimulationNode;
import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import java.util.List;

public class PollutionService extends AbstractSimCityService {
    private final CityMap map;

    public PollutionService(CityMap map) {
        this.map = map;
    }

    @Override
    public List<Class<? extends Component>> readComponents() {
        return List.of(BuildingComponent.class);
    }

    @Override
    public void beforeTick(int tickCount) {
        // Natural dissipation
        for (int x = 0; x < map.getWidth(); x++) {
            for (int y = 0; y < map.getHeight(); y++) {
                CityTile tile = map.getGrid()[x][y];
                tile.setAirPollution(Math.max(0, tile.getAirPollution() - 2));
                tile.setWaterPollution(Math.max(0, tile.getWaterPollution() - 1));
            }
        }
    }

    @Override
    protected void doProcessTile(CityTile tile, int tickCount) {
        int airGen = 0;
        int waterGen = 0;

        for (SimEntity entity : tile.getEntities()) {
            BuildingComponent building = entity.getComponent(BuildingComponent.class);
            if (building != null) {
                switch (building.getType()) {
                    case INDUSTRIAL -> {
                        airGen += 20;
                        waterGen += 10;
                    }
                    case POWER_PLANT -> {
                        airGen += 50;
                    }
                    case AGRICULTURAL -> {
                        waterGen += 15; // Pesticides/Fertilizers
                    }
                    case ROAD -> {
                        airGen += 2; // Traffic
                    }
                }
            }
        }

        if (airGen > 0 || waterGen > 0) {
            tile.setAirPollution(tile.getAirPollution() + airGen);
            tile.setWaterPollution(tile.getWaterPollution() + waterGen);

            // Spread to neighbors (simplified diffusion)
            for (SimulationNode<SimEntity> neighborNode : tile.getNeighbors()) {
                CityTile neighbor = (CityTile) neighborNode;
                neighbor.setAirPollution(neighbor.getAirPollution() + airGen / 4);
                neighbor.setWaterPollution(neighbor.getWaterPollution() + waterGen / 4);
            }
        }
    }
}
