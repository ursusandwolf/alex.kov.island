package com.island.engine.internal;

import com.island.engine.core.EntityIdProvider;
import com.island.engine.core.InternalEngine;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages unique entity IDs for SoA-based component stores.
 * Uses ConcurrentLinkedQueue for efficient ID recycling without global locks.
 */
@InternalEngine
public final class EntityIdManager implements EntityIdProvider {
    private final AtomicInteger nextId = new AtomicInteger(0);
    private final ConcurrentLinkedQueue<Integer> recycledIds = new ConcurrentLinkedQueue<>();

    @Override
    public int acquireId() {
        Integer id = recycledIds.poll();
        if (id != null) {
            return id;
        }
        return nextId.getAndIncrement();
    }

    public void releaseId(int id) {
        recycledIds.offer(id);
    }
}
