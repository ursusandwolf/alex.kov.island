package com.island.engine.ecs;

import com.island.engine.core.EngineAPI;
import java.util.BitSet;

/**
 * Represents a unique combination of component classes.
 * Used for high-performance entity grouping and querying.
 */
@EngineAPI
public record EntityArchetype(BitSet bitSet) {
    public EntityArchetype {
        // Defensive copy to ensure immutability
        bitSet = (BitSet) bitSet.clone();
    }

    /**
     * Checks if this archetype contains all components required by another bitset.
     */
    public boolean containsAll(BitSet required) {
        for (int i = required.nextSetBit(0); i >= 0; i = required.nextSetBit(i + 1)) {
            if (!bitSet.get(i)) {
                return false;
            }
        }
        return true;
    }
}
