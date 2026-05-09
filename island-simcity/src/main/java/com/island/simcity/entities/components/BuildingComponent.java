package com.island.simcity.entities.components;

import com.island.engine.ecs.Component;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Getter
@Setter
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

    private Type type;

    public Type getType() {
        return type;
    }

    public static BuildingComponentBuilder builder() {
        return new BuildingComponentBuilder();
    }

    public static class BuildingComponentBuilder {
        private Type type;

        public BuildingComponentBuilder type(Type type) {
            this.type = type;
            return this;
        }

        public BuildingComponent build() {
            return new BuildingComponent(type);
        }
    }
}
