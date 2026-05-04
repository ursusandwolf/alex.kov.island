package com.island.nature.entities;

/**
 * Reasons why an organism might die.
 */
public enum DeathCause {
    HUNGER("Hunger"),
    AGE("Age"),
    EATEN("Eaten"),
    EATEN_BY_PACK("Eaten by Pack"),
    MOVEMENT_EXHAUSTION("Exhaustion"),
    REPRODUCTION_EXHAUSTION("Reproduction Exhaustion");

    private final String displayName;

    DeathCause(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
