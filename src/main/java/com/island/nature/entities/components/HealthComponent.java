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
    private volatile long currentEnergy;
    private volatile long maxEnergy;
    private volatile boolean isAlive;

    public int getEnergyPercentage() {
        long max = maxEnergy;
        return (max == 0) ? 0 : (int) ((currentEnergy * 100) / max);
    }
}
