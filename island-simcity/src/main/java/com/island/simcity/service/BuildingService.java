package com.island.simcity.service;

import com.island.simcity.entities.SimEntity;
import com.island.simcity.entities.components.BuildingComponent;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildingService {
    private final CityMap map;

    public boolean build(int x, int y, BuildingComponent.Type type) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
            return false;
        }

        CityTile tile = map.getGrid()[x][y];

        // Cannot build on top of another building (simplified)
        boolean hasBuilding = tile.getEntities().stream()
                .anyMatch(e -> e.hasComponent(BuildingComponent.class));
        if (hasBuilding) {
            return false;
        }

        long cost = getCost(type);
        if (map.getMoney() < cost) {
            return false;
        }

        map.addMoney(-cost);
        SimEntity building = new SimEntity(map.getComponentRegistry());
        building.addComponent(BuildingComponent.builder().type(type).build());
        tile.addEntity(building);
        return true;
    }

    private long getCost(BuildingComponent.Type type) {
        return switch (type) {
            case ROAD -> 50;
            case RESIDENTIAL -> 200;
            case COMMERCIAL -> 500;
            case INDUSTRIAL -> 1000;
            case AGRICULTURAL -> 100;
            case RAILWAY -> 300;
            case METRO -> 2000;
            case WATER_PIPE -> 80;
        };
    }
}