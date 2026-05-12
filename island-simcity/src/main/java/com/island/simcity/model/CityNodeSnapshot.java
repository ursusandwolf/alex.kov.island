package com.island.simcity.model;

import com.island.engine.model.NodeSnapshot;
import com.island.simcity.entities.components.BuildingComponent;

import java.util.HashMap;
import java.util.Map;

public class CityNodeSnapshot implements NodeSnapshot {
    private final String coordinates;
    private final String topSpeciesCode;
    private final boolean hasOrganisms;
    private final Map<String, Integer> entityCounts;

    public CityNodeSnapshot(CityTile tile) {
        this.coordinates = tile.getX() + "," + tile.getY();
        
        Map<String, Integer> counts = new HashMap<>();
        String top = null;
        
        for (var e : tile.getEntities()) {
            BuildingComponent b = e.getComponent(BuildingComponent.class);
            if (b != null) {
                String type = b.getType().name().toLowerCase();
                counts.merge(type, 1, Integer::sum);
                if (top == null) {
                    top = type;
                }
            }
        }
        
        this.entityCounts = Map.copyOf(counts);
        this.topSpeciesCode = top;
        this.hasOrganisms = !tile.getEntities().isEmpty();
    }

    @Override
    public String getCoordinates() {
        return coordinates;
    }

    @Override
    public String getTopSpeciesCode() {
        return topSpeciesCode;
    }

    @Override
    public boolean isTopSpeciesPlant() {
        return false;
    }

    @Override
    public boolean hasOrganisms() {
        return hasOrganisms;
    }
    
    @Override
    public Map<String, Integer> getEntityCounts() {
        return entityCounts;
    }
}
