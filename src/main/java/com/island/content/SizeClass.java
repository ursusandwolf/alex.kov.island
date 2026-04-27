package com.island.content;

/**
 * Categorizes organisms by weight to unify metabolism and hunt logic.
 */
public enum SizeClass {
    SMALL,   // < 5.0 kg
    MEDIUM,  // 5.0 - 100.0 kg
    LARGE,   // 100.0 - 450.0 kg
    HUGE;    // > 450.0 kg

    public static SizeClass fromWeight(double weight) {
        if (weight < 5.0) return SMALL;
        if (weight < 100.0) return MEDIUM;
        if (weight < 450.0) return LARGE;
        return HUGE;
    }
}
