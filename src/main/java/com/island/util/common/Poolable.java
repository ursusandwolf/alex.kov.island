package com.island.util.common;

/**
 * Interface for objects that can be reset and reused in a pool.
 */
public interface Poolable {
    /**
     * Resets the object state to default for reuse.
     */
    void reset();
}