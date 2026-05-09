package com.island.engine.internal;

import com.island.engine.core.EntityIdProvider;
import com.island.engine.core.InternalEngine;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages unique entity IDs for SoA-based component stores.
 */
@InternalEngine
public class EntityIdManager implements EntityIdProvider {
    private final AtomicInteger nextId = new AtomicInteger(0);
    private final BitSet recycledIds = new BitSet();

    @Override
    public int acquireId() {
        synchronized (recycledIds) {
            int firstSetBit = recycledIds.nextSetBit(0);
            if (firstSetBit != -1) {
                recycledIds.clear(firstSetBit);
                return firstSetBit;
            }
        }
        return nextId.getAndIncrement();
    }

    public void releaseId(int id) {
        synchronized (recycledIds) {
            recycledIds.set(id);
        }
    }
}
