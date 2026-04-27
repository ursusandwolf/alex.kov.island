package com.island.content;

/**
 * Categorizes organisms by weight to unify metabolism and hunt logic.
 */
public enum SizeClass {
    SMALL(1.20, 2),   // < 5.0 kg
    MEDIUM(1.00, 1),  // 5.0 - 100.0 kg
    LARGE(0.80, 1),   // 100.0 - 450.0 kg
    HUGE(0.80, 1);    // > 450.0 kg

    private final double metabolismModifier;
    private final int offspringCount;

    SizeClass(double metabolismModifier, int offspringCount) {
        this.metabolismModifier = metabolismModifier;
        this.offspringCount = offspringCount;
    }

    public double getMetabolismModifier() {
        return metabolismModifier;
    }

    public int getOffspringCount() {
        return offspringCount;
    }

    public static SizeClass fromWeight(double weight) {
        if (weight < 6.0) { // WEIGHT_THRESHOLD_SMALL from SimulationConstants
            return SMALL;
        }
        if (weight < 100.0) {
            return MEDIUM;
        }
        if (weight < 450.0) {
            return LARGE;
        }
        return HUGE;
    }
}
