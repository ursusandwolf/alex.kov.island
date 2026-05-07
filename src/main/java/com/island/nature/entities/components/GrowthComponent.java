package com.island.nature.entities.components;

import com.island.engine.ecs.Component;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Component for entities that can grow (like Biomass).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GrowthComponent implements Component {
    private long growthRateBP;
    private long maxBiomass;
}
