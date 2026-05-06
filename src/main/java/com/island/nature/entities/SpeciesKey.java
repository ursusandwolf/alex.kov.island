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
