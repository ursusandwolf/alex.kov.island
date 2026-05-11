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
        AGRICULTURAL,
        ROAD,
        RAILWAY,
        METRO,
        WATER_PIPE,
        POWER_PLANT,
        POWER_LINE
    }

    public enum Density {
        LOW,
        MEDIUM,
        HIGH
    }

    private Type type;
    @Builder.Default
    private Density density = Density.LOW;
}
