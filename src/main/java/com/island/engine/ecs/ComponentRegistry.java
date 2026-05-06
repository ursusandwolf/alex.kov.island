package com.island.engine.ecs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry to map component classes to stable indices for ArrayComponentStore.
 */
public final class ComponentRegistry {
    private static final Map<Class<? extends Component>, Integer> indices = new ConcurrentHashMap<>();
    private static final AtomicInteger nextIndex = new AtomicInteger(0);

    public static int getIndex(Class<? extends Component> type) {
        return indices.computeIfAbsent(type, k -> nextIndex.getAndIncrement());
    }

    public static int getMaxIndex() {
        return nextIndex.get();
    }
    
    private ComponentRegistry() { }
}
