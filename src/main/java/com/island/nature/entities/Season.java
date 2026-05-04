package com.island.nature.entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents simulation seasons.
 */
@Getter
public enum Season {
    SPRING(1.0, 1.2),
    SUMMER(1.2, 1.0),
    AUTUMN(0.8, 0.8),
    WINTER(0.5, 1.5);

    private final double growthModifier;
    private final double metabolismModifier;

    Season(double growthModifier, double metabolismModifier) {
        this.growthModifier = growthModifier;
        this.metabolismModifier = metabolismModifier;
    }
}
