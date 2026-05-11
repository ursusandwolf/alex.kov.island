package com.island.simcity.model;

import com.island.engine.model.NodeSnapshot;
import com.island.simcity.entities.components.BuildingComponent;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CityNodeSnapshot implements NodeSnapshot {
    private final CityTile tile;

    @Override
    public String getCoordinates() {
        return tile.getX() + "," + tile.getY();
    }

    @Override
    public String getTopSpeciesCode() {
        return tile.getEntities().stream()
                .map(e -> e.getComponent(BuildingComponent.class))
                .filter(b -> b != null)
                .findFirst()
                .map(b -> b.getType().name().toLowerCase())
                .orElse(null);
    }

    @Override
    public boolean isTopSpeciesPlant() {
        return false;
    }

    @Override
    public boolean hasOrganisms() {
        return !tile.getEntities().isEmpty();
    }
}
