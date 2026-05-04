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
public class HealthComponent implements Component {
    private long currentEnergy;
    private long maxEnergy;
    private boolean isAlive;

    public int getEnergyPercentage() {
        return (maxEnergy == 0) ? 0 : (int) ((currentEnergy * 100) / maxEnergy);
    }
}
