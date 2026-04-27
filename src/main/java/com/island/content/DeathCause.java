package com.island.content;

/**
 * Reasons why an organism might die.
 */
public enum DeathCause {
    HUNGER("Голод"),
    AGE("Старость"),
    EATEN("Съеден"),
    MOVEMENT_EXHAUSTION("Утомление");

    private final String displayName;

    DeathCause(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
