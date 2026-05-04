package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class AgeComponent implements Component {
    private int age;
    private int maxLifespan;
}
