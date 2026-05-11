package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Component for entities that have metabolism.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MetabolismComponent implements Component {
    private long basalMetabolicRate;
}
