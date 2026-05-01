package com.island.simcity.service;

import com.island.simcity.entities.Building;
import com.island.simcity.model.CityMap;
import com.island.simcity.model.CityTile;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class BuildingService {
    private final CityMap map;

    public boolean build(int x, int y, Building.Type type) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
            return false;
        }

        CityTile tile = map.getGrid()[x][y];
        
        // Cannot build on top of another building (simplified)
        boolean hasBuilding = tile.getEntities().stream().anyMatch(e -> e instanceof Building);
        if (hasBuilding) {
            return false;
        }

        long cost = getCost(type);
        if (map.getMoney() < cost) {
            return false;
        }

        map.addMoney(-cost);
        tile.addEntity(new Building(type));
        return true;
    }

    private long getCost(Building.Type type) {
        return switch (type) {
            case ROAD -> 50;
            case RESIDENTIAL -> 200;
            case COMMERCIAL -> 500;
            case INDUSTRIAL -> 1000;
        };
    }
}
