package com.island.config;

/**
 * Categorized energy thresholds and costs to replace loose constants.
 */
public enum EnergyPolicy {
    ACTION_MIN(15.0),
    REPRODUCTION_MIN(70.0),
    BIRTH_INITIAL(50.0),
    ESCAPE_LOSS(5.0),
    REPRODUCTION_COST(15.0);

    private final double value;

    EnergyPolicy(double value) {
        this.value = value;
    }

    public double getPercent() {
        return value;
    }

    public double getFactor() {
        return value / 100.0;
    }
}
