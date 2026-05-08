package com.island.simcity.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Component for economic data in SimCity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EconomyComponent implements Component {
    private int wealth;
}
