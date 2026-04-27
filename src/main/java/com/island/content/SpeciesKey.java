package com.island.content;

/**
 * Enumeration of all species in the simulation to replace magic strings.
 */
public enum SpeciesKey {
    WOLF("wolf", true),
    BOA("boa", true),
    FOX("fox", true),
    BEAR("bear", true),
    EAGLE("eagle", true),
    
    HORSE("horse", false),
    DEER("deer", false),
    RABBIT("rabbit", false),
    MOUSE("mouse", false),
    GOAT("goat", false),
    SHEEP("sheep", false),
    BOAR("boar", false),
    BUFFALO("buffalo", false),
    DUCK("duck", false),
    CATERPILLAR("caterpillar", false),
    
    PLANT("plant", false),
    GRASS("grass", false),
    CABBAGE("cabbage", false);

    private final String code;
    private final boolean predator;

    SpeciesKey(String code, boolean predator) {
        this.code = code;
        this.predator = predator;
    }

    public String getCode() {
        return code;
    }

    public boolean isPredator() {
        return predator;
    }

    public boolean isPlant() {
        return this == PLANT || this == GRASS || this == CABBAGE;
    }

    public boolean isBiomass() {
        return isPlant() || this == CATERPILLAR;
    }


    public static SpeciesKey fromCode(String code) {
        for (SpeciesKey key : values()) {
            if (key.code.equalsIgnoreCase(code)) {
                return key;
            }
        }
        throw new IllegalArgumentException("Unknown species code: " + code);
    }
}
