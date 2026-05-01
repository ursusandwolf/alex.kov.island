package com.island.nature.entities;

/**
 * Reasons why an organism might die.
 */
public enum DeathCause {
    HUNGER("Hunger"),
    AGE("Age"),
    EATEN("Eaten"),
    MOVEMENT_EXHAUSTION("Exhaustion");

    private final String displayName;

    DeathCause(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
