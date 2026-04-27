package com.island.content;

/**
 * Categorizes organisms by weight to unify metabolism and hunt logic.
 */
public enum SizeClass {
    TINY(1.25, 5),    // < 1.0 kg (Mouse, Caterpillar)
    SMALL(1.20, 2),
    NORMAL(1.00, 1),
    MEDIUM(1.00, 1),
    LARGE(0.80, 1),   // 150.0 - 500.0 kg (Horse, Deer, Boar)
    HUGE(0.80, 1);    // > 500.0 kg (Bear, Buffalo)

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
        if (weight < 1.0) {
            return TINY;
        }
        if (weight < 5.0) {
            return SMALL;
        }
        if (weight < 10.0) {
            return NORMAL;
        }
        if (weight < 150.0) {
            return MEDIUM;
        }
        if (weight < 500.0) {
            return LARGE;
        }
        return HUGE;
    }
}
