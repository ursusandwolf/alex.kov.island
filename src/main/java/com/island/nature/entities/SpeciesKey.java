package com.island.nature.entities;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Value object representing a species key.
 * Hybrid Approach: Keeps core constants but flags are now in AnimalType.
 */
public record SpeciesKey(String code, boolean predator) implements Comparable<SpeciesKey> {
    
    public static final SpeciesKey WOLF = new SpeciesKey("wolf", true);
    public static final SpeciesKey BOA = new SpeciesKey("boa", true);
    public static final SpeciesKey FOX = new SpeciesKey("fox", true);
    public static final SpeciesKey BEAR = new SpeciesKey("bear", true);
    public static final SpeciesKey EAGLE = new SpeciesKey("eagle", true);
    
    public static final SpeciesKey HORSE = new SpeciesKey("horse", false);
    public static final SpeciesKey DEER = new SpeciesKey("deer", false);
    public static final SpeciesKey RABBIT = new SpeciesKey("rabbit", false);
    public static final SpeciesKey MOUSE = new SpeciesKey("mouse", false);
    public static final SpeciesKey HAMSTER = new SpeciesKey("hamster", false);
    public static final SpeciesKey GOAT = new SpeciesKey("goat", false);
    public static final SpeciesKey SHEEP = new SpeciesKey("sheep", false);
    public static final SpeciesKey BOAR = new SpeciesKey("boar", false);
    public static final SpeciesKey BUFFALO = new SpeciesKey("buffalo", false);
    public static final SpeciesKey DUCK = new SpeciesKey("duck", false);
    public static final SpeciesKey FROG = new SpeciesKey("frog", false);
    public static final SpeciesKey CHAMELEON = new SpeciesKey("chameleon", false);
    public static final SpeciesKey CATERPILLAR = new SpeciesKey("caterpillar", false);
    public static final SpeciesKey BUTTERFLY = new SpeciesKey("butterfly", false);
    
    public static final SpeciesKey PLANT = new SpeciesKey("plant", false);
    public static final SpeciesKey GRASS = new SpeciesKey("grass", false);
    public static final SpeciesKey MUSHROOM = new SpeciesKey("mushroom", false);

    public String getCode() {
        return code;
    }

    public boolean isPredator() {
        return predator;
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
