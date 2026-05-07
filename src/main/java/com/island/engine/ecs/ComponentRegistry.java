package com.island.engine.ecs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry to map component classes to stable indices for ArrayComponentStore.
 * Now an instance-based class to avoid global static state.
 */
public final class ComponentRegistry {
    private final Map<Class<? extends Component>, Integer> indices = new ConcurrentHashMap<>();
    private final AtomicInteger nextIndex = new AtomicInteger(0);

    public int getOrRegister(Class<? extends Component> type) {
        return indices.computeIfAbsent(type, k -> nextIndex.getAndIncrement());
    }

    public int size() {
        return nextIndex.get();
    }
}
