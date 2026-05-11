package com.island.simcity.entities.components;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Economic profile for building types to avoid magic numbers.
 */
@Getter
@RequiredArgsConstructor
public enum BuildingProfile {
    ROAD(1, 0),
    RESIDENTIAL(2, 0),
    COMMERCIAL(5, 500),
    INDUSTRIAL(10, 1000),
    AGRICULTURAL(1, 100),
    RAILWAY(5, 0),
    METRO(20, 0),
    WATER_PIPE(1, 0),
    POWER_PLANT(50, 0),
    POWER_LINE(2, 0);

    private final long baseExpense;
    private final long baseIncome;

    public static BuildingProfile of(BuildingComponent.Type type) {
        return valueOf(type.name());
    }

    public static long getDensityMultiplier(BuildingComponent.Density density) {
        return switch (density) {
            case LOW -> 1;
            case MEDIUM -> 4;
            case HIGH -> 12;
        };
    }
}
