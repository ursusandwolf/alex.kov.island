package com.island.simcity.entities.components;

import com.island.engine.ecs.Component;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuildingComponent implements Component {
    public enum Type {
        RESIDENTIAL,
        COMMERCIAL,
        INDUSTRIAL,
        ROAD
    }

    private Type type;
}
