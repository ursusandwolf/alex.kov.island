package com.island.simcity.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Component for building-related data in SimCity.
 */
@Getter
@AllArgsConstructor
public class BuildingComponent implements Component {
    public enum Type {
        RESIDENTIAL,
        COMMERCIAL,
        INDUSTRIAL,
        ROAD
    }

    private final Type type;
}
