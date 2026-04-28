package com.island.content;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Value object representing a species key.
 * Refactored from enum to record to allow dynamic species addition (OCP).
 */
public record SpeciesKey(String code, boolean predator) implements Comparable<SpeciesKey> {
    
    private static final Map<String, SpeciesKey> REGISTRY = new HashMap<>();

    public static final SpeciesKey WOLF = register("wolf", true);
    public static final SpeciesKey BOA = register("boa", true);
    public static final SpeciesKey FOX = register("fox", true);
    public static final SpeciesKey BEAR = register("bear", true);
    public static final SpeciesKey EAGLE = register("eagle", true);
    
    public static final SpeciesKey HORSE = register("horse", false);
    public static final SpeciesKey DEER = register("deer", false);
    public static final SpeciesKey RABBIT = register("rabbit", false);
    public static final SpeciesKey MOUSE = register("mouse", false);
    public static final SpeciesKey HAMSTER = register("hamster", false);
    public static final SpeciesKey GOAT = register("goat", false);
    public static final SpeciesKey SHEEP = register("sheep", false);
    public static final SpeciesKey BOAR = register("boar", false);
    public static final SpeciesKey BUFFALO = register("buffalo", false);
    public static final SpeciesKey DUCK = register("duck", false);
    public static final SpeciesKey FROG = register("frog", false);
    public static final SpeciesKey CHAMELEON = register("chameleon", false);
    public static final SpeciesKey CATERPILLAR = register("caterpillar", false);
    public static final SpeciesKey BUTTERFLY = register("butterfly", false);
    
    public static final SpeciesKey PLANT = register("plant", false);
    public static final SpeciesKey GRASS = register("grass", false);
    public static final SpeciesKey CABBAGE = register("cabbage", false);

    private static SpeciesKey register(String code, boolean predator) {
        SpeciesKey key = new SpeciesKey(code, predator);
        REGISTRY.put(code.toLowerCase(), key);
        return key;
    }

    public String getCode() {
        return code;
    }

    public boolean isPredator() {
        return predator;
    }

    public boolean isPlant() {
        return this.equals(PLANT) || this.equals(GRASS) || this.equals(CABBAGE);
    }

    public boolean isBiomass() {
        return isPlant() || this.equals(CATERPILLAR) || this.equals(BUTTERFLY);
    }

    public static SpeciesKey fromCode(String code) {
        SpeciesKey key = REGISTRY.get(code.toLowerCase());
        if (key == null) {
            // Allow dynamic creation if not in registry (OCP)
            return new SpeciesKey(code.toLowerCase(), false);
        }
        return key;
    }

    public static Collection<SpeciesKey> values() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    /**
     * Transitional method to replace enum ordinal() where absolutely necessary.
     * Better to use map lookup in performance-critical areas.
     */
    public int ordinal() {
        // This is a temporary hack for InteractionMatrix. 
        // In a real record-based system, we'd use a stable ID.
        return code.hashCode(); 
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SpeciesKey that = (SpeciesKey) o;
        return Objects.equals(code, that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public int compareTo(SpeciesKey other) {
        return this.code.compareTo(other.code);
    }
}
