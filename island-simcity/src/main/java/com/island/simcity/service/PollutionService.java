package com.island.simcity.service;

import com.island.engine.ecs.Component;
import com.island.engine.core.SimulationNode;
import com.island.engine.scheduling.Phase;
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
    public Phase phase() {
        return Phase.PREPARE;
    }

    @Override
    public int priority() {
        return 40; // After Connectivity (50)
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
                tile.setAirPollution(Math.max(0, tile.getAirPollution() - 5));
                tile.setWaterPollution(Math.max(0, tile.getWaterPollution() - 3));
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
                        airGen += 10;
                        waterGen += 5;
                    }
                    case POWER_PLANT -> {
                        airGen += 25;
                    }
                    case AGRICULTURAL -> {
                        waterGen += 8; // Pesticides/Fertilizers
                    }
                    case ROAD -> {
                        airGen += 1; // Traffic
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
