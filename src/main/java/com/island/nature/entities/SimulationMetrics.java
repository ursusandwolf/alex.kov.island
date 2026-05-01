package com.island.nature.entities;

import lombok.Builder;
import lombok.Value;

/**
 * Snapshot of global simulation metrics collected during a tick.
 */
@Value
@Builder
public class SimulationMetrics {
    long totalCurrentEnergy;
    long totalMaxEnergy;
    int animalCount;
    int starvingCount;

    public double getGlobalSatiety() {
        return (totalMaxEnergy == 0) ? 100.0 : ((double) totalCurrentEnergy / totalMaxEnergy) * 100.0;
    }

    public static SimulationMetrics empty() {
        return new SimulationMetrics(0, 0, 0, 0);
    }

    public static SimulationMetrics combine(SimulationMetrics m1, SimulationMetrics m2) {
        return SimulationMetrics.builder()
                .totalCurrentEnergy(m1.totalCurrentEnergy + m2.totalCurrentEnergy)
                .totalMaxEnergy(m1.totalMaxEnergy + m2.totalMaxEnergy)
                .animalCount(m1.animalCount + m2.animalCount)
                .starvingCount(m1.starvingCount + m2.starvingCount)
                .build();
    }
}
