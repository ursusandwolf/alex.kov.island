package com.island.nature.config;

/**
 * Categorized energy thresholds and costs using integer-based arithmetic.
 * Percent values are 0-100.
 */
public enum EnergyPolicy {
    ACTION_MIN(15),
    REPRODUCTION_MIN(70),
    BIRTH_INITIAL(50),
    ESCAPE_LOSS_BP(500), // 5%
    REPRODUCTION_COST_BP(1500); // 15%

    private final int value;

    EnergyPolicy(int value) {
        this.value = value;
    }

    public int getPercent() {
        return value;
    }

    /**
     * Returns the value as basis points (1/10000).
     */
    public int getBasisPoints() {
        if (name().endsWith("_BP")) {
            return value;
        }
        return value * 100;
    }
}
